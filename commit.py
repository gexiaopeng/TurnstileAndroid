#!/usr/bin/python
import sys
import os

if len(sys.argv) > 1:
	comment = str(sys.argv[1])
else:
	comment = ""
comment = "STORY=AI-website-1114 " + comment;
print comment
command = r'git commit -m "' + comment + '"';
os.system(command)
print command