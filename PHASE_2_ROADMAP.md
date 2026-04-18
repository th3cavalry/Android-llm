# Phase 2 Roadmap - Polish & Refinement

**Version:** 0.2.0  
**Target Release Date:** TBD  
**Status:** In Progress  
**Last Updated:** April 17, 2026

---

## Completion Status Legend
- ✅ **Completed** - Feature implemented and working
- 🚧 **Partial** - Some infrastructure exists but incomplete
- ❌ **Not Started** - Not yet implemented
- 🔄 **In Progress** - Currently being worked on

---

## Overview

Phase 2 focuses on improving the user experience, performance, and reliability of the app. This includes UI/UX enhancements, on-device performance optimizations, MCP server improvements, better testing, and documentation.

### Progress Summary (Updated April 17, 2026)

**Overall Completion:** 6/17 items complete (35%)

| Category | Status |
|----------|--------|
| ✅ **Completed** | 6 items (Model Download, MCP Test, Error Messages, Tool Loading, Session Management, Streaming Responses) |
| 🚧 **Partial** | 2 items (MCP Health, Error Tests) |
| ❌ **Not Started** | 9 items |

**Next Priority:** Sprint 2 - Continue Performance Optimization (Memory Management & Model Caching)

---

## 1. UI/UX Improvements

### Priority: High

#### 1.1 Error Messages Display ✅
- **Status:** COMPLETED (April 17, 2026)
- **Issue:** Error messages in chat are not clearly distinguishable from regular responses
- **Solution:** 
  - ✅ Add visual styling (red text, error icon) for error messages
  - ✅ Show error details in a expandable/collapsible format
  - ✅ Add retry button for transient errors
- **Files:** `ChatAdapter.kt`, `ChatViewModel.kt`, `item_message_error.xml`, `bg_message_error.xml`
- **Notes:** Error messages now appear as distinct chat bubbles with red styling, expandable stack traces, and retry functionality for network/timeout/connection errors

#### 1.2 Loading States for Tool Execution ✅
- **Status:** COMPLETED (April 17, 2026)
- **Issue:** No visual feedback when tools are being executed
- **Solution:**
  - ✅ Show loading spinner/progress indicator during tool execution
  - ✅ Display which tool is currently running
  - ✅ Show partial progress for long-running tools (SSH, GitHub operations)
- **Files:** `ChatAdapter.kt`, `ChatViewModel.kt`, `LLMService.kt`, `item_tool_executing.xml`
- **Notes:** Added executing state that appears between tool call and tool result, with spinner and contextual status messages based on tool type

#### 1.3 Model Download Progress ✅
- **Status:** COMPLETED
- **Issue:** Model browser doesn't show download progress
- **Solution:** 
  - ✅ Add progress bar in model download UI
  - ✅ Show percentage and estimated time remaining
  - ❌ Allow pausing/resuming downloads (not implemented)
- **Files:** `ModelBrowserActivity.kt`, `ModelBrowserViewModel.kt`
- **Notes:** `downloadProgress` StateFlow implemented, UI shows progress bar with percentage

#### 1.4 Chat Session Management ✅
- **Status:** COMPLETED (April 17, 2026)
- **Issue:** Users can't save, delete, or organize chat sessions
- **Solution:**
  - ✅ Add save/delete buttons for chat sessions
  - ✅ Allow renaming sessions
  - ✅ Implement session filtering/searching
  - ✅ Add export functionality (JSON/text)
- **Files:** `MainActivity.kt`, `ChatViewModel.kt`, `data/ChatSession.kt`, `SessionListAdapter.kt`, `dialog_session_list.xml`, `item_session.xml`
- **Notes:** Complete session management UI implemented with Material Design. Users can open/rename/delete/export sessions from context menu. Export supports JSON and TXT formats using Storage Access Framework.

#### 1.5 Settings UI Improvements ❌
- **Status:** Not started
- **Issue:** Settings screen is dense and overwhelming
- **Solution:**
  - Group settings into collapsible sections
  - Add tooltips/hints for each setting
  - Implement search/filter for settings
  - Add quick-access shortcuts for common tasks
- **Files:** `SettingsActivity.kt`

---

## 2. On-Device Performance

### Priority: High

#### 2.1 Streaming Responses for On-Device Models ✅
- **Status:** COMPLETED (April 17, 2026)
- **Issue:** On-device models only show full response after completion
- **Solution:**
  - ✅ Implement token-by-token streaming for MediaPipe backend
  - ✅ Implement streaming for LiteRT-LM backend (v0.10.0+ API)
  - ✅ Implement streaming for Gemini Nano backend
  - ✅ Update UI to show typing effect in real-time
- **Files:** `InferenceBackend.kt`, `OnDeviceInferenceService.kt`, `LiteRtLmBackend.kt`, `GeminiNanoBackend.kt`, `ChatViewModel.kt`, `ChatAdapter.kt`, `ChatMessage.kt`
- **Notes:** All three backends now support token-by-token streaming via `generateStream()` method returning Flow<String>. UI shows real-time typing effect with cursor indicator (▋) while streaming. Messages marked with `isStreaming` flag and updated incrementally as tokens arrive.

#### 2.2 Better Memory Management ❌
- **Status:** Not started
- **Issue:** Large models can cause OOM errors on low-RAM devices
- **Solution:**
  - Implement model unloading when not in use
  - Add memory usage monitoring and warnings
  - Implement context window management (sliding window)
  - Add device compatibility checks before model load
- **Files:** `InferenceBackend.kt`, `ChatViewModel.kt`, `ModelBrowserViewModel.kt`

#### 2.3 Model Caching/Persistence ❌
- **Status:** Not started
- **Issue:** Models must be reloaded on every app restart
- **Solution:**
  - Keep model in memory across app lifecycle
  - Implement background model preloading
  - Add model warm-up on app launch
- **Files:** `InferenceBackend.kt`, `ChatViewModel.kt`

---

## 3. MCP Server Enhancements

### Priority: Medium

#### 3.1 MCP Server Discovery/Browsing UI ❌
- **Status:** Not started (manual entry only)
- **Issue:** Users must manually enter MCP server URLs
- **Solution:**
  - Add public MCP server registry/browser
  - Show popular/pre-configured servers
  - Add server categories/tags
  - ✅ Implement server testing before save (test button exists)
- **Files:** `MCPManagerActivity.kt`, `MCPClient.kt`
- **Notes:** Manual entry dialog works, test function exists but no discovery/browsing

#### 3.2 MCP Server Health/Status Indicators 🚧
- **Status:** Partial (test function exists but no persistent status)
- **Issue:** No visibility into MCP server connection status
- **Solution:**
  - ❌ Add connection status indicator per server
  - ❌ Show last connection time and errors
  - ❌ Implement automatic reconnection on failure
  - ✅ Add manual test/reconnect button (exists)
- **Files:** `MCPManagerActivity.kt`, `MCPClient.kt`, `MCPServer.kt`
- **Notes:** `testServer()` function can test connections but no persistent status display

#### 3.3 Better MCP Tool Error Handling ❌
- **Status:** Not started
- **Issue:** MCP tool errors are not user-friendly
- **Solution:**
  - Parse and display structured error messages from MCP servers
  - Add retry mechanism for transient errors
  - Show helpful hints for common error types
- **Files:** `ToolExecutor.kt`, `MCPClient.kt`

---

## 4. Testing & Quality

### Priority: Medium

#### 4.1 Integration Tests for Key Workflows ❌
- **Status:** Not started (no androidTest files exist)
- **Issue:** No integration tests for critical user flows
- **Solution:**
  - Test chat workflow (send message → receive response)
  - Test tool execution workflow
  - Test model download workflow
  - Test MCP server connection workflow
- **Files:** `app/src/androidTest/`
- **Notes:** No androidTest directory or files found

#### 4.2 UI Tests for Settings and Model Browser ❌
- **Status:** Not started (no androidTest files exist)
- **Issue:** No automated UI tests for key screens
- **Solution:**
  - Test Settings activity (backend selection, model path)
  - Test Model Browser (search, download, set path)
  - Test MCP Manager (add, edit, delete servers)
- **Files:** `app/src/androidTest/`
- **Notes:** No androidTest directory or files found

#### 4.3 More Error Scenario Coverage 🚧
- **Status:** Partial (one test file exists)
- **Issue:** Error handling paths not fully tested
- **Solution:**
  - Test network failure scenarios
  - Test invalid API key scenarios
  - Test model load failure scenarios
  - Test MCP server connection failure scenarios
  - ✅ Add unit test coverage for error taxonomy (partial)
- **Files:** `app/src/test/`, `data/Errors.kt`
- **Notes:** `ErrorsTest.kt` exists with basic tests but coverage is limited

---

## 5. Documentation

### Priority: Low

#### 5.1 In-App Help/Tutorials ❌
- **Status:** Not started
- **Issue:** New users don't know how to get started
- **Solution:**
  - Add onboarding flow for first-time users
  - Create inline help for each setting
  - Add tutorial videos/screenshots
  - Implement "What's New" dialog for each version
- **Files:** `MainActivity.kt`, `SettingsActivity.kt`

#### 5.2 Better MCP Server Documentation ❌
- **Status:** Not started
- **Issue:** Users don't know which MCP servers to try
- **Solution:**
  - Add pre-configured server templates
  - Create server configuration guide
  - Add examples for common MCP use cases
  - Document MCP server setup process
- **Files:** `MCPManagerActivity.kt`, `README.md`

#### 5.3 Troubleshooting Guide ❌
- **Status:** Not started
- **Issue:** Users struggle to debug issues
- **Solution:**
  - Create comprehensive troubleshooting section
  - Add common error messages and solutions
  - Implement in-app "Diagnostics" screen
  - Add log export functionality
- **Files:** `README.md`, `AboutActivity.kt`

---

## Implementation Order

### Sprint 1 (Weeks 1-2): UI/UX Foundation 🔄
**Status:** In Progress - 2/3 complete

1. ✅ Error messages display (1.1) - **COMPLETED**
2. ✅ Loading states for tool execution (1.2) - **COMPLETED**
3. 🚧 Chat session management (1.4) - **NEXT TASK** - Complete the UI

**Priority:** These are foundational UX improvements that affect every user interaction.

### Sprint 2 (Weeks 3-4): Performance & Streaming
**Status:** Not Started
**Dependencies:** Sprint 1 complete

1. ❌ Streaming responses for on-device models (2.1)
2. ❌ Better memory management (2.2)
3. ❌ Model caching/persistence (2.3)

### Sprint 3 (Weeks 5-6): MCP & Testing
**Status:** Not Started  
**Dependencies:** Sprint 2 complete

1. 🚧 MCP server health/status indicators (3.2) - Complete persistent display
2. ❌ Integration tests (4.1)
3. ❌ UI tests for Settings (4.2)

### Sprint 4 (Weeks 7-8): Polish & Docs
**Status:** Not Started
**Dependencies:** Sprint 3 complete

1. ❌ Settings UI improvements (1.5)
2. ❌ MCP server discovery (3.1)
3. ❌ In-app help/tutorials (5.1)
4. ❌ Troubleshooting guide (5.3)

---

## Success Metrics

- **User Satisfaction:** >4.5/5 star rating
- **Error Rate:** <1% of chat interactions result in errors
- **Performance:** On-device model response time <5 seconds
- **Test Coverage:** >80% unit test coverage
- **Documentation:** All features documented with examples

---

## Notes

- Phase 2 is focused on refinement, not new features
- All changes should be backward compatible
- Performance improvements should not break existing functionality
- UI changes should follow Material Design 3 guidelines
