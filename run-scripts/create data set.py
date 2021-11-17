import os,glob
import sys

folder_path = sys.argv[1]

res = sys.argv[2]

out_file = open(res, 'a+')
for filename in glob.glob(folder_path + "*"):
    out_file.write(filename + "\n")
out_file.close()
