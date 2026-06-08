package com.heightmeter.app.measurement.model

/**
 * Represents a 3D point in real-world space.
 * Used to store measurement points placed by the user.
 * Coordinates are in meters.
 */
data class Point3D(
    val id: Long = System.nanoTime(),
    val x: Float,
    val y: Float,
    val z: Float,
    val anchorId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        /** Distance threshold for considering two points as the same location (in meters) */
        private const val SNAP_THRESHOLD = 0.01f

        /**
         * Check if two points are approximately equal (within snap threshold).
         */
        fun areApproxEqual(a: Point3D, b: Point3D): Boolean {
            val dx = a.x - b.x
            val dy = a.y - b.y
            val dz = a.z - b.z
            return (dx * dx + dy * dy + dz * dz) < (SNAP_THRESHOLD * SNAP_THRESHOLD)
        }

        val ZERO: Point3D = Point3D(x = 0f, y = 0f, z = 0f)
    }
}
