# Phase 2 Roadmap - Polish & Refinement

**Version:** 0.2.0  
**Target Release Date:** TBD  
**Status:** Planning Phase

---

## Overview

Phase 2 focuses on improving the user experience, performance, and reliability of the app. This includes UI/UX enhancements, on-device performance optimizations, MCP server improvements, better testing, and documentation.

---

## 1. UI/UX Improvements

### Priority: High

#### 1.1 Error Messages Display
- **Issue:** Error messages in chat are not clearly distinguishable from regular responses
- **Solution:** 
  - Add visual styling (red text, error icon) for error messages
  - Show error details in a expandable/collapsible format
  - Add retry button for transient errors
- **Files:** `ChatAdapter.kt`, `ChatViewModel.kt`

#### 1.2 Loading States for Tool Execution
- **Issue:** No visual feedback when tools are being executed
- **Solution:**
  - Show loading spinner/progress indicator during tool execution
  - Display which tool is currently running
  - Show partial progress for long-running tools (SSH, GitHub operations)
- **Files:** `ChatAdapter.kt`, `MainActivity.kt`

#### 1.3 Model Download Progress
- **Issue:** Model browser doesn't show download progress
- **Solution:**
  - Add progress bar in model download UI
  - Show percentage and estimated time remaining
  - Allow pausing/resuming downloads
- **Files:** `ModelBrowserActivity.kt`, `ModelBrowserViewModel.kt`

#### 1.4 Chat Session Management
- **Issue:** Users can't save, delete, or organize chat sessions
- **Solution:**
  - Add save/delete buttons for chat sessions
  - Allow renaming sessions
  - Implement session filtering/searching
  - Add export functionality (JSON/text)
- **Files:** `MainActivity.kt`, `ChatViewModel.kt`, `data/ChatSession.kt`

#### 1.5 Settings UI Improvements
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

#### 2.1 Streaming Responses for On-Device Models
- **Issue:** On-device models only show full response after completion
- **Solution:**
  - Implement token-by-token streaming for MediaPipe backend
  - Implement streaming for LiteRT-LM backend (v0.11.0+ API)
  - Implement streaming for Gemini Nano backend
  - Update UI to show typing effect in real-time
- **Files:** `OnDeviceInferenceService.kt`, `LiteRtLmBackend.kt`, `GeminiNanoBackend.kt`, `ChatViewModel.kt`

#### 2.2 Better Memory Management
- **Issue:** Large models can cause OOM errors on low-RAM devices
- **Solution:**
  - Implement model unloading when not in use
  - Add memory usage monitoring and warnings
  - Implement context window management (sliding window)
  - Add device compatibility checks before model load
- **Files:** `InferenceBackend.kt`, `ChatViewModel.kt`, `ModelBrowserViewModel.kt`

#### 2.3 Model Caching/Persistence
- **Issue:** Models must be reloaded on every app restart
- **Solution:**
  - Keep model in memory across app lifecycle
  - Implement background model preloading
  - Add model warm-up on app launch
- **Files:** `InferenceBackend.kt`, `ChatViewModel.kt`

---

## 3. MCP Server Enhancements

### Priority: Medium

#### 3.1 MCP Server Discovery/Browsing UI
- **Issue:** Users must manually enter MCP server URLs
- **Solution:**
  - Add public MCP server registry/browser
  - Show popular/pre-configured servers
  - Add server categories/tags
  - Implement server testing before save
- **Files:** `MCPManagerActivity.kt`, `MCPClient.kt`

#### 3.2 MCP Server Health/Status Indicators
- **Issue:** No visibility into MCP server connection status
- **Solution:**
  - Add connection status indicator per server
  - Show last connection time and errors
  - Implement automatic reconnection on failure
  - Add manual test/reconnect button
- **Files:** `MCPManagerActivity.kt`, `MCPClient.kt`, `MCPServer.kt`

#### 3.3 Better MCP Tool Error Handling
- **Issue:** MCP tool errors are not user-friendly
- **Solution:**
  - Parse and display structured error messages from MCP servers
  - Add retry mechanism for transient errors
  - Show helpful hints for common error types
- **Files:** `ToolExecutor.kt`, `MCPClient.kt`

---

## 4. Testing & Quality

### Priority: Medium

#### 4.1 Integration Tests for Key Workflows
- **Issue:** No integration tests for critical user flows
- **Solution:**
  - Test chat workflow (send message → receive response)
  - Test tool execution workflow
  - Test model download workflow
  - Test MCP server connection workflow
- **Files:** `app/src/androidTest/`

#### 4.2 UI Tests for Settings and Model Browser
- **Issue:** No automated UI tests for key screens
- **Solution:**
  - Test Settings activity (backend selection, model path)
  - Test Model Browser (search, download, set path)
  - Test MCP Manager (add, edit, delete servers)
- **Files:** `app/src/androidTest/`

#### 4.3 More Error Scenario Coverage
- **Issue:** Error handling paths not fully tested
- **Solution:**
  - Test network failure scenarios
  - Test invalid API key scenarios
  - Test model load failure scenarios
  - Test MCP server connection failure scenarios
  - Add unit test coverage for error taxonomy
- **Files:** `app/src/test/`, `data/Errors.kt`

---

## 5. Documentation

### Priority: Low

#### 5.1 In-App Help/Tutorials
- **Issue:** New users don't know how to get started
- **Solution:**
  - Add onboarding flow for first-time users
  - Create inline help for each setting
  - Add tutorial videos/screenshots
  - Implement "What's New" dialog for each version
- **Files:** `MainActivity.kt`, `SettingsActivity.kt`

#### 5.2 Better MCP Server Documentation
- **Issue:** Users don't know which MCP servers to try
- **Solution:**
  - Add pre-configured server templates
  - Create server configuration guide
  - Add examples for common MCP use cases
  - Document MCP server setup process
- **Files:** `MCPManagerActivity.kt`, `README.md`

#### 5.3 Troubleshooting Guide
- **Issue:** Users struggle to debug issues
- **Solution:**
  - Create comprehensive troubleshooting section
  - Add common error messages and solutions
  - Implement in-app "Diagnostics" screen
  - Add log export functionality
- **Files:** `README.md`, `AboutActivity.kt`

---

## Implementation Order

### Sprint 1 (Weeks 1-2): UI/UX Foundation
1. Error messages display (1.1)
2. Loading states for tool execution (1.2)
3. Chat session management (1.4)

### Sprint 2 (Weeks 3-4): Performance & Streaming
1. Streaming responses for on-device models (2.1)
2. Better memory management (2.2)
3. Model caching/persistence (2.3)

### Sprint 3 (Weeks 5-6): MCP & Testing
1. MCP server health/status indicators (3.2)
2. Integration tests (4.1)
3. UI tests for Settings (4.2)

### Sprint 4 (Weeks 7-8): Polish & Docs
1. Settings UI improvements (1.5)
2. MCP server discovery (3.1)
3. In-app help/tutorials (5.1)
4. Troubleshooting guide (5.3)

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
