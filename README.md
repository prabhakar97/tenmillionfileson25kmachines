# Introduction
This repo is one solution of the 10 million files on 25k machines problem. It uses concurrency primitives from Java to implement a multithreaded concurrent solution.

# The Problem
You have 25000 machines on which 10 million files are replicated - i.e. there are files numbered from 1 to 10000000 
and content of file with same name on all the machines is exactly the same. Each file has a set of integers in them, one per line.
You have been given a client machine where you have a library exposing the RPC call `long getSum(int machineId, int fileId)` which is expected to 
make an RPC call on the given machine to get the sum of all integers in the file named fileId. This is a synchronous blocking call in the library and is subject
to failures like timeout and arbitrary delays.
Write a program that can be run on a single client machine that can calculate the sum of integers in every file

# Solution
This solution uses `BlockingQueue<T>`, `ConcurrentMap<K, V>`, `ConcurrentQueue<T>`, `AtomicLong`, `Semaphore` and `ReentrantLock` concurrency primitives from JDK8
and above to achieve maximum parallelism given the constraints.

## Invariant
At any point of time there are max `degreeOfParallelism` number of threads (plus a constant number of extra threads for bookkeeping) running on the machine.
Asusming 100 as this config parameter, 100 threads fire up requests to 100 machines to get the sum of files assigned to them. When a thread has got the result,
it puts it into a `BlockingQueue` from which another thread keeps pulling out items and adding to the running total.

## Current State
The current commit is not the most efficient solution. I might improve things in future upon availability of time.
