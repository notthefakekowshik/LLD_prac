---
description: Initialize codebase context for LLD_prac repo
---

# Initialize Repo Context

Run this at the start of new chats to load project context.

## Steps

1. **Index the repo structure**
   - Run: `find . -type f -name "*.java" -o -name "*.md" | head -50`
   
2. **Get main directories**
   - Run: `ls -la`
   
3. **Read key documentation**
   - Read: `README.md` if it exists
   
4. **Identify patterns**
   - Search for design patterns being practiced
   - Look for example implementations

## Usage

In a new chat, type `/init` in the Windsurf command palette to run this workflow automatically.
