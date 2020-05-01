#!/usr/bin/python3
# -*- coding: utf-8 -*-
from datetime import datetime, date
import os
import os.path
import sys
import platform
from os import environ
from pathlib import Path
print("cwd")
print(os.getcwd())
print("home: %s\npath.home: %s"%(os.path.expanduser('~'),Path.home()))
print("argv: %s"%sys.argv)
#print("platform: %s\nsystem: %s\nuname: %s\njava_ver: %s\nwin32_ver: %s\nlibc_ver: %s"
#      %(platform.platform(),platform.system(),platform.uname(),platform.java_ver()
#        ,platform.win32_ver(),platform.libc_ver()))
print("platform: %s\nsystem: %s\nuname: %s"
      %(platform.platform(),platform.system(),platform.uname()))
print("architecture: %s\nmachine: %s\nnode: %s"
      %(platform.architecture(),platform.machine(),platform.node()))
print("environ:%s\ndist:%s"%(environ,platform.dist()))
#        ,platform.java_ver(),platform.win32_ver(),platform.libc_ver()))
#fd = open("/storage/emulated/0/Download/test.txt", 'w+')
print("opening...")
fd = open("/storage/emulated/0/Android/data/com.example.sztab/files/MyFileStorage/szconfig.properties", 'w+')
print("writing")
fd.write(str(datetime.now()))
#print("reading...")
#line = fd.read().split("\n")[0] #str(datetime.now()))
#print("line read: " + line)
print("closing")
fd.close()
print("exiting")
