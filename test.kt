fun main() {
    val b = mutableSetOf<String>()
    b.add("B1")
    b.add("B3")
    println(b.joinToString(" + "))
}
