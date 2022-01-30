import os

def metis_to_txt(filename):
    i=1
    edges = dict()
    f = open("heuristic_public/" + filename, 'r')
    w = open("out/" + filename, 'w+')
    for line in f:
        if (i == 1 or line == "" or "%" in line):
            i+=1
            continue
        if not line.startswith('%'):
            edges[i]= set()
            fields=line.split()
            for field in fields:
                w.write(str(i - 1) + " " + str(field) + "\n")
        i+=1

with os.scandir("heuristic_public") as it:
    for entry in it:
        if entry.is_file():
            print(entry.name)
            metis_to_txt(entry.name)