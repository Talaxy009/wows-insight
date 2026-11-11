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
        if days.count > 1 || !categories.isEmpty {
            VStack(spacing: 16) {
                if days.count > 1 {
                    TrendCard(days: days, mode: mode, metric: $metric)
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
    let mode: BattleMode
    @Binding var metric: ChartMetric

    private var points: [TrendPoint] {
        days.compactMap { day in
            day.modes[mode].map { TrendPoint(day: day.day, stats: $0) }
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            ChartHeader(title: L10n.Chart.trendTitle) {
                Picker(L10n.Chart.trendTitle, selection: $metric) {
                    ForEach(ChartMetric.allCases) { option in
                        Text(option.label).tag(option)
                    }
                }
                .pickerStyle(.segmented)
                .labelsHidden()
            }

            Chart(points) { point in
                let value = metric.value(of: point.stats)

                // The implicit baseline of an area is the bottom of the chart
                // frame, not of the plot, which paints over the date labels;
                // anchoring it to the y domain keeps the fill inside the plot.
                AreaMark(
                    x: .value(L10n.Chart.day, point.day),
                    yStart: .value(metric.label, yDomain.lowerBound),
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
            .chartXScale(domain: xDomain)
            .chartYScale(domain: yDomain)
            .chartXAxis {
                AxisMarks(values: .stride(by: .day, count: dayStride)) {
                    AxisGridLine()
                    AxisValueLabel(format: .dateTime.month(.abbreviated).day())
                }
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

    private var xDomain: ClosedRange<Date> {
        guard let first = points.first?.day, let last = points.last?.day else {
            let now = Date()
            return now...now
        }
        // A little room on both ends keeps the first and last dot off the edge
        // without adding a tick label outside the recorded days.
        let inset: TimeInterval = 5 * 60 * 60
        return first.addingTimeInterval(-inset)...last.addingTimeInterval(inset)
    }

    /// Ticks land on whole days only, so a short series never prints the same
    /// date twice for two tick times of one day.
    private var dayStride: Int {
        guard let first = points.first?.day, let last = points.last?.day else { return 1 }
        let days = Calendar.current.dateComponents([.day], from: first, to: last).day ?? 1
        return max(1, Int((Double(days) / 4).rounded(.up)))
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
            ChartHeader(title: L10n.Chart.breakdownTitle) {
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

// MARK: - Shared

/// A card headline with the control that switches what the chart plots.
private struct ChartHeader<Control: View>: View {
    let title: String
    let control: Control

    init(title: String, @ViewBuilder control: () -> Control) {
        self.title = title
        self.control = control()
    }

    var body: some View {
        HStack(spacing: 12) {
            Text(title)
                .font(.headline)
            Spacer(minLength: 8)
            control
                .frame(maxWidth: 180)
        }
    }
}

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
