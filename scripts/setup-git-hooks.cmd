@echo off
rem Enables the repository git hooks (.githooks): commit messages must follow Conventional Commits.
git -C "%~dp0.." config core.hooksPath .githooks && echo Git hooks enabled (.githooks).
