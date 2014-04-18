package schematic.common

/**
 * Convenience trait for easy class creation and caching. This will be useful in the future for
 * easy mocking in unit tests.
 * @author tlei (Terence Lei)
 */
trait CachedObject[A] {
    private var accessor: Option[A] = None

    protected def create: A

    def apply(): A = {
        accessor match {
            case Some(ret) =>
                ret
            case None =>
                val ret = create
                accessor = Some(ret)
                ret
        }
    }
}
