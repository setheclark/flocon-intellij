package io.github.setheclark.intellij.mcp.prompts

import io.modelcontextprotocol.kotlin.sdk.types.*

/**
 * Prompt: understand_screen_apis
 *
 * Guides the LLM through analyzing which APIs are called when a specific screen loads.
 * Uses time-based clustering to identify screen boundaries and call dependencies.
 */
fun createUnderstandScreenApisPrompt() = Prompt(
    name = "understand_screen_apis",
    description = "Analyze which APIs are called during a specific screen or feature interaction, including dependencies and optimization opportunities.",
    arguments = listOf(
        PromptArgument(
            name = "screenName",
            description = "Description of the screen or feature (e.g., 'Profile Screen', 'Login Flow')",
            required = true
        ),
        PromptArgument(
            name = "startTime",
            description = "When you started viewing the screen (epoch milliseconds)",
            required = true
        ),
        PromptArgument(
            name = "endTime",
            description = "When you finished viewing the screen (epoch milliseconds)",
            required = true
        )
    )
)

/**
 * Handler for the understand_screen_apis prompt.
 */
fun handleUnderstandScreenApisPrompt(request: GetPromptRequest): GetPromptResult {
    val screenName = request.arguments?.get("screenName") ?: "Unknown Screen"
    val startTime = request.arguments?.get("startTime") ?: "MISSING"
    val endTime = request.arguments?.get("endTime") ?: "MISSING"

    val workflow = """
You are analyzing the API calls for: **$screenName**

Time range: $startTime to $endTime (epoch milliseconds)

Follow this workflow to understand the screen's network behavior:

## Step 1: Retrieve All Calls in Time Range
Use `get_network_calls` with:
- startTimeAfter: $startTime
- startTimeBefore: $endTime
- format: "detailed" (to see durations and timing)

Sort mentally by startTime to see the call sequence.

## Step 2: Cluster Calls by Time Proximity
Group calls that occur close together (within ~2 seconds):
- **Initial Load:** Calls in first 0-2 seconds
- **Lazy Loading:** Calls 2-5 seconds after initial load
- **User Interactions:** Calls after 5+ seconds or with large gaps

Identify:
- How many distinct "waves" of calls occurred
- Whether calls are parallel (same time) or sequential (waterfall)

## Step 3: Analyze Each Cluster
For each cluster of calls:

**Identify Purpose:**
- What data is each endpoint fetching?
- Based on URL and response, infer what UI element it populates
- Example: `/api/user/profile` → User name, avatar
- Example: `/api/posts` → Feed content

**Detect Dependencies:**
- Sequential calls often indicate dependencies
- If Call B starts after Call A completes, check if:
  - Call B uses data from Call A's response (ID, token, etc.)
  - They could actually run in parallel

**Measure Performance:**
- Which calls are slowest?
- Are there waterfalls that could be flattened?
- Could parallel calls be batched?

## Step 4: Identify Screen Transitions
Look for gaps > 5 seconds between call clusters:
- These likely indicate user interactions or screen changes
- Helps distinguish screen load from subsequent user actions

## Step 5: Optimization Opportunities
Suggest improvements:

**Parallelization:**
- If sequential calls have no dependency, they should run in parallel
- Example: User profile and settings can fetch simultaneously

**Batching:**
- If multiple small requests go to same service, batch into one
- Example: Multiple GraphQL queries → single batched query

**Prefetching:**
- If data is always needed, fetch it earlier
- Example: Prefetch detail view data while list is loading

**Caching:**
- If same data is requested multiple times, cache it
- Check for redundant calls to same endpoint

**Critical Path:**
- Identify which calls block UI rendering
- Suggest moving non-critical calls to background

## Step 6: Document the Flow
Create a clear description of the screen's API flow:

1. **Initial Load (0-2s):**
   - List all calls with purpose
   - Note parallel vs sequential
   - Highlight any blocking waterfalls

2. **Lazy Loading (2-5s):** (if applicable)
   - What loads after initial render?

3. **User Interactions:** (if applicable)
   - What APIs fire on button clicks, etc.?

## Output Format
Present as:
1. **Summary:** Total calls, time to complete, success rate
2. **Call Sequence:** Chronological list with timing and purpose
3. **Dependency Graph:** Which calls depend on others (if any)
4. **Performance Analysis:** Slowest calls, bottlenecks
5. **Optimization Recommendations:** Specific, prioritized suggestions
6. **Visual Timeline:** Text-based representation of when calls occur

Make it actionable for developers to optimize the screen's performance.
""".trimIndent()

    return GetPromptResult(
        description = "Analyzing API usage for $screenName",
        messages = listOf(
            PromptMessage(
                role = Role.User,
                content = TextContent(workflow)
            )
        )
    )
}
