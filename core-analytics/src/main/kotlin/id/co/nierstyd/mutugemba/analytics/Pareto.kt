package id.co.nierstyd.mutugemba.analytics

fun paretoCounts(items: List<String>): List<Pair<String, Int>> =
    items
        .groupingBy { it }
        .eachCount()
        .toList()
        .sortedWith(
            compareByDescending<Pair<String, Int>> { it.second }
                .thenBy { it.first },
        )
