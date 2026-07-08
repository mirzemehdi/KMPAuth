package com.mmk.kmpauth.firebase.aggregator

/**
 * Placeholder so every Kotlin target of this source-less backward-compat
 * aggregator produces a compilation artifact.
 *
 * `:deprecated:kmpauth-firebase` re-exports the granular Firebase modules
 * purely through `api(...)` dependencies and has no code of its own. Without
 * at least one source file, the Kotlin/Native compilations are skipped as
 * `NO-SOURCE`, so no `.klib` is produced and
 * `generateMetadataFileFor<Target>Publication` fails during publishing with a
 * missing-klib `FileNotFoundException`. This internal marker keeps each
 * compilation non-empty; it is not part of the public API.
 */
internal val kmpAuthFirebaseAggregatorMarker: Unit = Unit
