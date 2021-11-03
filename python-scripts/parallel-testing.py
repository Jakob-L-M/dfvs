from pebble import concurrent
from concurrent.futures import TimeoutError
import subprocess as sp
import multiprocessing as mp
import time

timeout = 2

@concurrent.process(timeout=2)
def solve_graph(file):
    return sp.check_output(["bash", "run", file, str(timeout)])

def worker(file):
    start_time = time.time()
    future = solve_graph(file)

    try:
        result = future.result()  # blocks until results are ready
        print([len(result.decode("utf-8").split("\n")), time.time() - start_time])
        return [len(result.decode("utf-8").split("\n")), time.time() - start_time]
    except TimeoutError as error:
        print([-1, timeout])
        return [-1, timeout]
    except Exception as error:
        print("Function raised %s" % error)
        print(error.traceback)  # traceback of the function

if __name__ == '__main__':

    files = []

    for line in open("data-list.txt", 'r'):
        if not line.startswith("#"):
            files += [line.strip()]
        
    files = files[:5]
    count = 8
    pool = mp.Pool(processes=count)
    
    pool.map(worker, files)
    