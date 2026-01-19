package io.github.setheclark.intellij.mcp.prompts

import io.modelcontextprotocol.kotlin.sdk.types.*

/**
 * Prompt: find_redundant_apis
 *
 * Guides the LLM through analyzing network calls to identify redundant/duplicate API requests.
 * The LLM performs the actual analysis - we just provide the workflow guidance.
 */
fun createFindRedundantApisPrompt() = Prompt(
    name = "find_redundant_apis",
    description = "Analyze network calls to identify redundant or duplicate API requests that could be optimized through caching or deduplication.",
    arguments = listOf(
        PromptArgument(
            name = "timeWindowHours",
            description = "How many hours of history to analyze (default: 24)",
            required = false
        )
    )
)

/**
 * Handler for the find_redundant_apis prompt.
 */
fun handleFindRedundantApisPrompt(request: GetPromptRequest): GetPromptResult {
    val timeWindowHours = request.arguments?.get("timeWindowHours")?.toIntOrNull() ?: 24
    val timeWindowMs = timeWindowHours * 60 * 60 * 1000L
    val startTime = System.currentTimeMillis() - timeWindowMs

    val workflow = """
You are analyzing network calls to find redundant API requests. Follow this workflow:

## Step 1: Get Network Calls
Use the `get_network_calls` tool with these parameters:
- startTimeAfter: $startTime (covers last $timeWindowHours hours)
- format: "summary" (for efficient browsing)

## Step 2: Identify Potential Duplicates
Analyze the results to find:
- **Exact duplicates:** Same URL and method called multiple times
- **Similar requests:** Same endpoint with different parameters
- **GraphQL duplicates:** Same GraphQL operation called repeatedly

Look for patterns like:
- Multiple calls to the same endpoint within seconds/minutes
- Repeated queries that could be cached
- Sequential calls that could be batched

## Step 3: Validate Duplicates
For the top redundancy candidates:
- Use `get_network_call_details` to fetch full details for the duplicate calls
- Verify they truly return the same data
- Check if request parameters are identical

## Step 4: Calculate Impact
For each redundancy pattern:
- Count the number of duplicate calls
- Sum the durations of duplicate calls (after the first) to calculate wasted time
- Estimate the data transfer overhead

## Step 5: Provide Recommendations
For each redundancy pattern, suggest:
- **Caching:** If data doesn't change frequently, implement client-side caching
- **Deduplication:** If multiple components request same data, use a request deduplicator
- **Prefetching:** If data is always needed together, fetch it once upfront
- **Batching:** If multiple similar requests occur, batch them into one call

## Output Format
Present findings as:
1. Summary: Total redundant calls found, total wasted time
2. Top redundancy patterns (ranked by wasted time)
3. For each pattern:
   - Request signature (method + URL)
   - Number of occurrences
   - Wasted time and data transfer
   - Example call IDs
   - Specific optimization recommendation

Focus on actionable insights that developers can immediately implement.
""".trimIndent()

    return GetPromptResult(
        description = "Workflow for finding redundant API calls in the last $timeWindowHours hours",
        messages = listOf(
            PromptMessage(
                role = Role.User,
                content = TextContent(workflow)
            )
        )
    )
}
