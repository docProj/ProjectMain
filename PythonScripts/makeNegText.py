from __future__ import print_function
from glob import glob
import os


my_folder = 'G:/weightDetection/negative'
f = open('G:/weightDetection/bg.txt', 'w')

files_list = glob(os.path.join(my_folder, '*.jpg'))
for a_file in sorted(files_list):
  split = a_file.split('\\')
  f.write("negative/" + split[1] + "\n")
