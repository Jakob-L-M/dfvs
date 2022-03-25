# Instances

In this folder we collect instance files, solution files and the best known solutions ever found.

Instances can be downloaded [here](https://drive.google.com/file/d/1oufl99P3K_3rNxFhQkGlcUkkBDpd7RDH/view?usp=sharing).

The folder sturcture should look like this:
```
instances/
|- instances/
|   |- graph files in PACE format
|- solutions/
|   |- solutions files in PACE format
|- .gitignore
|- best_known.txt
|- README.md
|- verifyer.exe
```

The `best_known.txt` file is automaticly updatet by our Java code and should not be adjusted manually. Solution files are overwritten each time a new model is testet. We will not share instances or solutions here.

The `verifyer` can be downloaded directly from the pace website. It is used to validate solutions before (potentialy) adjusting `best_known.txt`.