import Foundation

extension Error {
    /// `true` when a failure only means the surrounding task was cancelled, so
    /// the caller should drop the result instead of showing an error.
    ///
    /// `URLSession` cancellation arrives as `CancellationError` (see
    /// `YuyukoClient`), while other requests can fail after the task was
    /// cancelled and still raise a regular error.
    var isCancellation: Bool {
        self is CancellationError || Task.isCancelled
    }
}
