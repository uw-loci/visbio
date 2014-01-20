#!/usr/bin/python

###
# #%L
# VisBio application for visualization of multidimensional biological
# image data.
# %%
# Copyright (C) 2002 - 2014 Board of Regents of the University of
# Wisconsin-Madison.
# %%
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as
# published by the Free Software Foundation, either version 2 of the
# License, or (at your option) any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public
# License along with this program.  If not, see
# <http://www.gnu.org/licenses/gpl-2.0.html>.
# #L%
###

from PIL import Image
import os
import sys
percent = .9

def resize (filename):
  im = Image.open(filename);
  size = im.size
  newWidth = int(size[0] * percent)
  newHeight = int(size[1] * percent)
  newSize = newWidth, newHeight
  newIm = im.resize(newSize)
  im.save("bak." + filename);
  newIm.save(filename);

# ask about resizing file
print sys.argv[1]
s = raw_input("resize image? ")
if s == "y":
  resize(sys.argv[1]);
else:
  print sys.argv[1] + "skipped"

