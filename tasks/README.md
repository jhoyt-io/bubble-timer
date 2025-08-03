# Task Management System

This directory contains our lightweight task management system for tracking work on the Bubble Timer project.

## Structure

- `active/` - Tasks currently being worked on
- `done/` - Completed tasks and their summaries

## Workflow

1. **New Tasks**: Create new task files in `active/` using `TASK_TEMPLATE.md` as a starting point
2. **Active Work**: Update task files with progress, notes, and status changes
3. **Completion**: When a task is done, move it to `done/` and optionally create a summary file
4. **Summaries**: Implementation summaries and retrospectives go in `done/`

## Task File Format

Each task should include:
- **Name**: Clear, descriptive title
- **Description**: What the task involves
- **Status**: Current state (Not Started, In Progress, Blocked, Completed)
- **Initial Prompt**: The original user request
- **Notes/Updates**: Progress updates and decisions made
- **Related Files**: Links to relevant code or documentation

## Benefits

- Keeps the root directory clean
- Provides a searchable history of work done
- Allows for easy tracking of ongoing work
- Maintains context for future reference 