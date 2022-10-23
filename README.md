# Gitlet
Gitlet is a version-control system that mimics some of the basic features of the popular system Git.

## Commands
### init 
Usage: `java gitlet.Main init`  
Function: Creates a new Gitlet version-control system in the current directory.

### add
Usage: `java gitlet.Main add [file name]`  
Function: Adds a copy of the file as it currently exists to the staging area.

### commit
Usage: `java gitlet.Main commit [message]`  
Function: Saves a snapshot of tracked files in the current commit and staging area so they can be restored at a later time, creating a new commit.

### rm
Usage: `java gitlet.Main rm [file name]`  
Function: Unstage the file if it is currently staged for addition.

### log
Usage: `java gitlet.Main log`    
Function: Starting at the current head commit, display information about each commit backwards along the commit tree until the initial commit, following the first parent commit links, ignoring any second parents found in merge commits.

### global-log
Usage: `java gitlet.Main global-log`  
Function: Like log, except displays information about all commits ever made.

### find
Usage: `java gitlet.Main find [commit message]`
Function: Prints out the ids of all commits that have the given commit message.

### status
Usage: `java gitlet.Main status`  
Function: Displays what branches currently exist, and marks the current branch with a *. Also displays what files have been staged for addition or removal.

### checkout
Usage:
1. `java gitlet.Main checkout -- [file name]`
2. `java gitlet.Main checkout [commit id] -- [file name]`
3. `java gitlet.Main checkout [branch name]`  
Functions: 
1. Takes the version of the file as it exists in the head commit and puts it in the working directory, overwriting the version of the file that’s already there if there is one.
2. Takes the version of the file as it exists in the commit with the given id, and puts it in the working directory, overwriting the version of the file that’s already there if there is one.
3. Takes all files in the commit at the head of the given branch, and puts them in the working directory, overwriting the versions of the files that are already there if they exist. 


### branch
Usage: `java gitlet.Main branch [branch name]`  
Function: Creates a new branch with the given name, and points it at the current head commit.

### rm-branch
Usage: `java gitlet.Main rm-branch [branch name]`  
Function: Deletes the branch with the given name.

### reset
Usage: `java gitlet.Main reset [commit id]`  
Function: Checks out all the files tracked by the given commit. Removes tracked files that are not present in that commit. Also moves the current branch’s head to that commit node.

### merge
Usage: `java gitlet.Main merge [branch name]`  
Function: Merges files from the given branch into the current branch.

