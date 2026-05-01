---
description: Initialize codebase context for LLD_prac repo
---

# Initialize Repo Context

Run this at the start of new chats to load project context.

## Repo Root

Determine `{repo_root}` from current working directory:
- MacBook Pro: `/Users/knarayanam/backup_folder/MY_FILES/LLD_prac`
- Mac Mini: `/Volumes/Crucial_X9/LLD_prep`

## Steps

1. **Index the repo structure**
   - Run: `find {repo_root} -type f \( -name "*.java" -o -name "*.md" \) | head -50`
   
2. **Get main directories**
   - Run: `ls -la {repo_root}`
   
3. **Read key documentation**
   - Read: `{repo_root}/README.md` if it exists
   
4. **Identify patterns**
   - Search for design patterns being practiced
   - Look for example implementations

## Usage

In a new chat, type `/init` in the Windsurf command palette to run this workflow automatically.
