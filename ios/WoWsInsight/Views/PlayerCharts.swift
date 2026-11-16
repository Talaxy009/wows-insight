import Charts
import SwiftUI

/// The chart cards of the player page: the daily trend of the battle type on
/// screen and the breakdown of it by ship type or tier.
///
/// Cards that have nothing to draw stay hidden, so a player the service has no
/// record of does not see an empty chart.
struct PlayerChartsSection: View {
    let report: PlayerReport
    let mode: BattleMode
    /// Owned by the player screen so scrolling the lazy stack does not reset
    /// what the cards plot.
    @Binding var metric: ChartMetric
    @Binding var breakdown: ChartBreakdown

    var body: some View {
        let days = report.dailyStats(for: mode)
        let categories = report.categories(.shipType, for: mode)
        // One record is a number, not a trend.
        let hasTrend = days.count > 1

        if hasTrend || !categories.isEmpty {
            VStack(spacing: 16) {
                if hasTrend {
                    TrendCard(
                        days: days,
                        window: report.dailyStats,
                        mode: mode,
                        metric: $metric
                    )
                }
                if !categories.isEmpty {
                    BreakdownCard(report: report, mode: mode, breakdown: $breakdown)
                }
            }
        }
    }
}

// MARK: - Trend

/// The rating or win rate of every recorded day, oldest on the left.
private struct TrendCard: View {
    let days: [DailyStats]
    /// Every day the service returned, not only the ones with battles in this
    /// mode: the axis ends on the newest of them even when that day has no
    /// battles, so a gap at the right edge does not move the ticks.
    let window: [DailyStats]
    let mode: BattleMode
    @Binding var metric: ChartMetric

    private var points: [TrendPoint] {
        days.compactMap { day in
            day.modes[mode].map { TrendPoint(day: day.day, stats: $0) }
        }
    }

    /// The service keeps the last week, so the axis always spans seven days
    /// ending on the last recorded one. That puts the every-other-day ticks on
    /// days 1 / 3 / 5 / 7 whether or not every day has battles.
    private static let axisDays = 7

    var body: some View {
        // Every mark below reads the scale, so it is worked out once rather
        // than per mark.
        let domain = yDomain

        VStack(alignment: .leading, spacing: 16) {
            CardHeader(title: L10n.Chart.trendTitle) {
                Picker(L10n.Chart.trendTitle, selection: $metric) {
                    ForEach(ChartMetric.allCases) { option in
                        Text(option.label).tag(option)
                    }
                }
                .pickerStyle(.segmented)
                .labelsHidden()
            }

            Chart {
                ForEach(points) { point in
                    let value = metric.value(of: point.stats)

                    // The implicit baseline of an area is the bottom of the
                    // chart frame, not of the plot, which paints over the date
                    // labels; anchoring it to the y domain keeps the fill inside
                    // the plot.
                    AreaMark(
                        x: .value(L10n.Chart.day, point.day),
                        yStart: .value(metric.label, domain.lowerBound),
                        yEnd: .value(metric.label, value)
                    )
                    .interpolationMethod(.monotone)
                    .foregroundStyle(areaGradient)

                    LineMark(
                        x: .value(L10n.Chart.day, point.day),
                        y: .value(metric.label, value)
                    )
                    .interpolationMethod(.monotone)
                    .foregroundStyle(lineColor)
                    .lineStyle(StrokeStyle(lineWidth: 2, lineCap: .round))

                    PointMark(
                        x: .value(L10n.Chart.day, point.day),
                        y: .value(metric.label, value)
                    )
                    .foregroundStyle(pointColor(for: point))
                    .symbolSize(44)
                }

                // Dashed day lines as marks rather than axis grid lines: an
                // axis grid line is drawn on the edge of its tick's band, while
                // the date is plotted in the middle of it, so the line would
                // miss the dots below.
                ForEach(tickDays, id: \.self) { day in
                    RuleMark(x: .value(L10n.Chart.day, day))
                        .foregroundStyle(gridColor)
                        .lineStyle(StrokeStyle(lineWidth: 1, dash: [3, 3]))
                }
            }
            .chartXScale(domain: xDomain)
            .chartYScale(domain: domain)
            // The dates are laid out by `xAxisLabels` instead of an `AxisMarks`
            // label: an axis label is centred in its tick's band, half a band
            // away from the day the mark is drawn at, and no anchor option
            // removes that offset. Placing the text at the proxy's x for the day
            // puts it on the same column as the dashed line and the dot.
            .chartXAxis(.hidden)
            .chartPlotStyle { plot in
                plot.padding(.bottom, Self.xAxisLabelHeight)
            }
            .chartOverlay { proxy in
                xAxisLabels(proxy: proxy)
            }
            .chartYAxis {
                AxisMarks(position: .leading, values: .automatic(desiredCount: 4)) { value in
                    AxisGridLine()
                    AxisValueLabel {
                        if let number = value.as(Double.self) {
                            Text(metric.axisText(number))
                                .font(.footnote)
                                .monospacedDigit()
                        }
                    }
                }
            }
            .chartLegend(.hidden)
            .frame(height: 170)
        }
        .cardStyle()
    }

    private var lineColor: Color { Color.accentColor }

    private var areaGradient: LinearGradient {
        LinearGradient(
            colors: [lineColor.opacity(0.28), lineColor.opacity(0.02)],
            startPoint: .top,
            endPoint: .bottom
        )
    }

    private func pointColor(for point: TrendPoint) -> Color {
        switch metric {
        case .rating: point.stats.rating.tier.color
        case .winRate: lineColor
        }
    }

    private var windowEnd: Date? { window.last?.day ?? points.last?.day }

    private var windowStart: Date? {
        guard let end = windowEnd else { return nil }
        return Calendar.current.date(byAdding: .day, value: 1 - Self.axisDays, to: end)
    }

    private var xDomain: ClosedRange<Date> {
        let first = windowStart ?? .now
        let last = windowEnd ?? first
        // Half a day on both ends keeps the first and last dot off the edge and
        // leaves the outermost date enough room to stay centred on its line.
        let inset: TimeInterval = 12 * 60 * 60
        return first.addingTimeInterval(-inset)...last.addingTimeInterval(inset)
    }

    /// The days that get a dashed line and a label: days 1, 3, 5 and 7.
    private var tickDays: [Date] {
        guard let first = windowStart else { return [] }
        let calendar = Calendar.current
        // Stepping through the calendar keeps the ticks on midnight across a
        // daylight-saving change; adding 86 400 s would drift by an hour.
        return stride(from: 0, to: Self.axisDays, by: 2).compactMap { offset in
            calendar.date(byAdding: .day, value: offset, to: first)
        }
    }

    private var gridColor: Color {
        Color(uiColor: .separator).opacity(0.6)
    }

    /// Space reserved below the plot for the dates.
    private static let xAxisLabelHeight: CGFloat = 20

    private func xAxisLabels(proxy: ChartProxy) -> some View {
        GeometryReader { geometry in
            let plot = proxy.plotFrame.map { geometry[$0] } ?? .zero
            ForEach(tickDays, id: \.self) { day in
                if let x = proxy.position(forX: day) {
                    let text = day.formatted(.dateTime.month(.abbreviated).day())
                    Text(text)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .fixedSize()
                        // Centred on the line, even where that puts a few
                        // points of the first or last date over the gutter or
                        // the card padding: shifting it would break the column
                        // the label shares with the line and the dot.
                        .position(
                            x: plot.minX + x,
                            y: plot.maxY + Self.xAxisLabelHeight / 2
                        )
                }
            }
        }
    }

    /// Room above and below the line so that a flat series does not sit on the
    /// axis, and so that a point on 0 % or 100 % is not cut in half.
    private var yDomain: ClosedRange<Double> {
        let values = points.map { metric.value(of: $0.stats) }
        guard let low = values.min(), let high = values.max() else { return 0...1 }

        let padding = max((high - low) * 0.25, metric == .rating ? 60 : 8)
        switch metric {
        case .rating:
            let lower = max(low - padding, 0)
            let upper = high + padding
            return lower...max(upper, lower + 1)
        case .winRate:
            // Let the scale run a little past 0 and 100; the ticks stay on the
            // round percentages inside it.
            let lower = max(low - padding, -5)
            let upper = min(high + padding, 105)
            return lower...max(upper, lower + 1)
        }
    }
}

private struct TrendPoint: Identifiable {
    let day: Date
    let stats: ModeStats

    var id: Date { day }
}

// MARK: - Breakdown

/// The personal rating of every ship type or tier the player fought in.
///
/// Swift Charts places the category labels of a horizontal bar chart inside the
/// plot area on iOS 26, where they collide with the bars, so the rows are drawn
/// directly: the same rounded bar language as the rest of the app, with the
/// rating tier colour.
private struct BreakdownCard: View {
    let report: PlayerReport
    let mode: BattleMode
    @Binding var breakdown: ChartBreakdown

    var body: some View {
        let categories = report.categories(breakdown, for: mode)
        let scale = scaleMaximum(in: categories)

        VStack(alignment: .leading, spacing: 16) {
            CardHeader(title: L10n.Chart.breakdownTitle) {
                Picker(L10n.Chart.breakdownTitle, selection: $breakdown) {
                    ForEach(ChartBreakdown.allCases) { option in
                        Text(option.label).tag(option)
                    }
                }
                .pickerStyle(.segmented)
                .labelsHidden()
            }

            VStack(spacing: 10) {
                ForEach(categories) { category in
                    BreakdownRow(
                        category: category,
                        rating: category.modes[mode]?.rating ?? .unknown,
                        scale: scale
                    )
                }
            }
        }
        .cardStyle()
    }

    /// The longest bar fills the row, leaving the rest for the value.
    private func scaleMaximum(in categories: [CategoryStats]) -> Double {
        Double(categories.compactMap { $0.modes[mode]?.rating.value }.max() ?? 0)
    }
}

private struct BreakdownRow: View {
    let category: CategoryStats
    let rating: PlayerRating
    let scale: Double

    var body: some View {
        HStack(spacing: 10) {
            Text(category.label)
                .font(.footnote)
                .foregroundStyle(.secondary)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
                .frame(width: 92, alignment: .leading)

            GeometryReader { proxy in
                let ratio = scale > 0 ? Double(max(rating.value, 0)) / scale : 0
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(Color.appPlaceholder)
                    Capsule()
                        .fill(rating.tier.color)
                        .frame(width: max(proxy.size.width * ratio, 4))
                }
            }
            .frame(height: 12)

            Text(rating.displayText)
                .font(.statLabel)
                .foregroundStyle(.secondary)
                .monospacedDigit()
                .frame(width: 52, alignment: .trailing)
        }
        .accessibilityElement(children: .combine)
    }
}

// MARK: - Axis labels

private extension ChartMetric {
    /// Y-axis label: a plain number for the rating, a percentage for the win
    /// rate.
    func axisText(_ value: Double) -> String {
        switch self {
        case .rating: StatFormat.integer(Int(value.rounded()))
        case .winRate: StatFormat.percent(value, digits: 0)
        }
    }
}
