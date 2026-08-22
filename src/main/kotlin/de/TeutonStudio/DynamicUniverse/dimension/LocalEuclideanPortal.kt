package de.TeutonStudio.DynamicUniverse.dimension

/**
 * The vertical face of a dimension. A planet stack is ordered from its core
 * upwards into space, so an inner layer meets an outer layer at UPPER -> LOWER.
 */
data class LocalPortalEndpoint(
    val dimension: DimensionId,
    val face: DimensionBoundaryFace,
)

/**
 * A directed, horizontal, locally Euclidean portal at one shared dimension
 * boundary. Implementations can use this specification to create their portal
 * entity without having to infer a direction from dimension ids or list order.
 */
data class LocalEuclideanPortal(
    val id: String,
    val source: LocalPortalEndpoint,
    val target: LocalPortalEndpoint,
    val boundary: BoundarySurface,
    val scale: DimensionScale,
) {
    init {
        require(id.matches(Regex("[a-z0-9_./:-]+"))) { "Invalid portal id: $id" }
        require(source.dimension != target.dimension) { "A portal needs two different dimensions." }
        require(source.face != target.face) { "A vertical portal must join opposite faces." }
    }

    fun targetPosition(position: DimensionPosition): DimensionPosition = position.copy(
        x = scale.map(position.x),
        z = scale.map(position.z),
    )
}

/**
 * Lookup view used by a portal adapter when it encounters a bottom or top
 * boundary in a loaded level. Each endpoint is deliberately unique: otherwise
 * the surrounding vertical stack cannot be determined unambiguously.
 */
class LocalEuclideanPortalGraph(portals: Collection<LocalEuclideanPortal>) {
    private val bySource = portals.associateBy(LocalEuclideanPortal::source)

    init {
        require(bySource.size == portals.size) {
            "A dimension face may belong to only one vertical portal."
        }
    }

    fun portalAt(dimension: DimensionId, face: DimensionBoundaryFace): LocalEuclideanPortal? =
        bySource[LocalPortalEndpoint(dimension, face)]

    fun portalsFrom(dimension: DimensionId): List<LocalEuclideanPortal> =
        DimensionBoundaryFace.entries.mapNotNull { portalAt(dimension, it) }
}
