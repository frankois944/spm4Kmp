package io.github.frankois944.spmForKmp.utils

// Function to extract all .target blocks
internal fun extractTargetBlocks(content: String): List<String> {
    val targets = mutableListOf<String>()
    val targetPattern = Regex("""\.target\s*\(""") // Matches the start of a .target block
    val startMatches = targetPattern.findAll(content)

    for (match in startMatches) {
        val startIndex = match.range.first
        val remainingString = content.substring(startIndex)
        val targetBlock = extractFullBlock(remainingString)
        if (targetBlock != null) {
            targets.add(targetBlock)
        }
    }
    return targets
}

// Function to extract the full block, ensuring nested parentheses are handled
internal fun extractFullBlock(content: String): String? {
    val openParen = '('
    val closeParen = ')'
    var openCount = 0
    val blockBuilder = StringBuilder()

    for (char in content) {
        blockBuilder.append(char)
        if (char == openParen) {
            openCount++
        } else if (char == closeParen) {
            openCount--
            if (openCount == 0) {
                // We've closed the block
                return blockBuilder.toString()
            }
        }
    }
    // If we exit the loop and openCount != 0, the block is incomplete
    return null
}
