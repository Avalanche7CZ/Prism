package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class MultiLockFairBlockingQueue implements BlockingQueue {
   final int LOCK_COUNT = Runtime.getRuntime().availableProcessors();
   final AtomicInteger putQueue = new AtomicInteger(0);
   final AtomicInteger pollQueue = new AtomicInteger(0);
   private final ReentrantLock[] locks;
   final LinkedList[] items;
   final LinkedList[] waiters;

   public int getNextPut() {
      int idx = Math.abs(this.putQueue.incrementAndGet()) % this.LOCK_COUNT;
      return idx;
   }

   public int getNextPoll() {
      int idx = Math.abs(this.pollQueue.incrementAndGet()) % this.LOCK_COUNT;
      return idx;
   }

   public MultiLockFairBlockingQueue() {
      this.locks = new ReentrantLock[this.LOCK_COUNT];
      this.items = new LinkedList[this.LOCK_COUNT];
      this.waiters = new LinkedList[this.LOCK_COUNT];

      for(int i = 0; i < this.LOCK_COUNT; ++i) {
         this.items[i] = new LinkedList();
         this.waiters[i] = new LinkedList();
         this.locks[i] = new ReentrantLock(false);
      }

   }

   public boolean offer(Object e) {
      int idx = this.getNextPut();
      ReentrantLock lock = this.locks[idx];
      lock.lock();
      ExchangeCountDownLatch c = null;

      try {
         if (this.waiters[idx].size() > 0) {
            c = (ExchangeCountDownLatch)this.waiters[idx].poll();
            c.setItem(e);
         } else {
            this.items[idx].addFirst(e);
         }
      } finally {
         lock.unlock();
      }

      if (c != null) {
         c.countDown();
      }

      return true;
   }

   public boolean offer(Object e, long timeout, TimeUnit unit) throws InterruptedException {
      return this.offer(e);
   }

   public Object poll(long timeout, TimeUnit unit) throws InterruptedException {
      int idx = this.getNextPoll();
      Object result = null;
      ReentrantLock lock = this.locks[idx];
      boolean error = true;
      lock.lock();

      try {
         result = this.items[idx].poll();
         if (result == null && timeout > 0L) {
            ExchangeCountDownLatch c = new ExchangeCountDownLatch(1);
            this.waiters[idx].addLast(c);
            lock.unlock();
            if (!c.await(timeout, unit)) {
               lock.lock();
               this.waiters[idx].remove(c);
               lock.unlock();
            }

            result = c.getItem();
         } else {
            lock.unlock();
         }

         error = false;
      } finally {
         if (error && lock.isHeldByCurrentThread()) {
            lock.unlock();
         }

      }

      return result;
   }

   public Future pollAsync() {
      int idx = this.getNextPoll();
      Future result = null;
      ReentrantLock lock = this.locks[idx];
      boolean error = true;
      lock.lock();

      try {
         Object item = this.items[idx].poll();
         if (item == null) {
            ExchangeCountDownLatch c = new ExchangeCountDownLatch(1);
            this.waiters[idx].addLast(c);
            lock.unlock();
            result = new ItemFuture(c);
         } else {
            lock.unlock();
            result = new ItemFuture(item);
         }

         error = false;
      } finally {
         if (error && lock.isHeldByCurrentThread()) {
            lock.unlock();
         }

      }

      return result;
   }

   public boolean remove(Object e) {
      for(int idx = 0; idx < this.LOCK_COUNT; ++idx) {
         ReentrantLock lock = this.locks[idx];
         lock.lock();

         boolean var5;
         try {
            boolean result = this.items[idx].remove(e);
            if (!result) {
               continue;
            }

            var5 = result;
         } finally {
            lock.unlock();
         }

         return var5;
      }

      return false;
   }

   public int size() {
      int size = 0;

      for(int idx = 0; idx < this.LOCK_COUNT; ++idx) {
         size += this.items[idx].size();
      }

      return size;
   }

   public Iterator iterator() {
      return new FairIterator();
   }

   public Object poll() {
      int idx = this.getNextPoll();
      ReentrantLock lock = this.locks[idx];
      lock.lock();

      Object var3;
      try {
         var3 = this.items[idx].poll();
      } finally {
         lock.unlock();
      }

      return var3;
   }

   public boolean contains(Object e) {
      for(int idx = 0; idx < this.LOCK_COUNT; ++idx) {
         boolean result = this.items[idx].contains(e);
         if (result) {
            return result;
         }
      }

      return false;
   }

   public boolean add(Object e) {
      return this.offer(e);
   }

   public int drainTo(Collection c, int maxElements) {
      throw new UnsupportedOperationException("int drainTo(Collection<? super E> c, int maxElements)");
   }

   public int drainTo(Collection c) {
      return this.drainTo(c, Integer.MAX_VALUE);
   }

   public void put(Object e) throws InterruptedException {
      this.offer(e);
   }

   public int remainingCapacity() {
      return Integer.MAX_VALUE - this.size();
   }

   public Object take() throws InterruptedException {
      return this.poll(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
   }

   public boolean addAll(Collection c) {
      Iterator i = c.iterator();

      while(i.hasNext()) {
         Object e = i.next();
         this.offer(e);
      }

      return true;
   }

   public void clear() {
      throw new UnsupportedOperationException("void clear()");
   }

   public boolean containsAll(Collection c) {
      throw new UnsupportedOperationException("boolean containsAll(Collection<?> c)");
   }

   public boolean isEmpty() {
      return this.size() == 0;
   }

   public boolean removeAll(Collection c) {
      throw new UnsupportedOperationException("boolean removeAll(Collection<?> c)");
   }

   public boolean retainAll(Collection c) {
      throw new UnsupportedOperationException("boolean retainAll(Collection<?> c)");
   }

   public Object[] toArray() {
      throw new UnsupportedOperationException("Object[] toArray()");
   }

   public Object[] toArray(Object[] a) {
      throw new UnsupportedOperationException("<T> T[] toArray(T[] a)");
   }

   public Object element() {
      throw new UnsupportedOperationException("E element()");
   }

   public Object peek() {
      throw new UnsupportedOperationException("E peek()");
   }

   public Object remove() {
      throw new UnsupportedOperationException("E remove()");
   }

   protected class FairIterator implements Iterator {
      Object[] elements = null;
      int index;
      Object element = null;

      public FairIterator() {
         ArrayList list = new ArrayList(MultiLockFairBlockingQueue.this.size());

         for(int idx = 0; idx < MultiLockFairBlockingQueue.this.LOCK_COUNT; ++idx) {
            ReentrantLock lock = MultiLockFairBlockingQueue.this.locks[idx];
            lock.lock();

            try {
               this.elements = (Object[])(new Object[MultiLockFairBlockingQueue.this.items[idx].size()]);
               MultiLockFairBlockingQueue.this.items[idx].toArray(this.elements);
            } finally {
               lock.unlock();
            }
         }

         this.index = 0;
         this.elements = (Object[])(new Object[list.size()]);
         list.toArray(this.elements);
      }

      public boolean hasNext() {
         return this.index < this.elements.length;
      }

      public Object next() {
         if (!this.hasNext()) {
            throw new NoSuchElementException();
         } else {
            this.element = this.elements[this.index++];
            return this.element;
         }
      }

      public void remove() {
         for(int idx = 0; idx < MultiLockFairBlockingQueue.this.LOCK_COUNT; ++idx) {
            ReentrantLock lock = MultiLockFairBlockingQueue.this.locks[idx];
            lock.lock();

            try {
               boolean result = MultiLockFairBlockingQueue.this.items[idx].remove(this.elements[this.index]);
               if (result) {
                  break;
               }
            } finally {
               lock.unlock();
            }
         }

      }
   }

   protected class ExchangeCountDownLatch extends CountDownLatch {
      protected volatile Object item;

      public ExchangeCountDownLatch(int i) {
         super(i);
      }

      public Object getItem() {
         return this.item;
      }

      public void setItem(Object item) {
         this.item = item;
      }
   }

   protected class ItemFuture implements Future {
      protected volatile Object item = null;
      protected volatile ExchangeCountDownLatch latch = null;
      protected volatile boolean canceled = false;

      public ItemFuture(Object item) {
         this.item = item;
      }

      public ItemFuture(ExchangeCountDownLatch latch) {
         this.latch = latch;
      }

      public boolean cancel(boolean mayInterruptIfRunning) {
         return false;
      }

      public Object get() throws InterruptedException, ExecutionException {
         if (this.item != null) {
            return this.item;
         } else if (this.latch != null) {
            this.latch.await();
            return this.latch.getItem();
         } else {
            throw new ExecutionException("ItemFuture incorrectly instantiated. Bug in the code?", new Exception());
         }
      }

      public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
         if (this.item != null) {
            return this.item;
         } else if (this.latch != null) {
            boolean timedout = !this.latch.await(timeout, unit);
            if (timedout) {
               throw new TimeoutException();
            } else {
               return this.latch.getItem();
            }
         } else {
            throw new ExecutionException("ItemFuture incorrectly instantiated. Bug in the code?", new Exception());
         }
      }

      public boolean isCancelled() {
         return false;
      }

      public boolean isDone() {
         return this.item != null || this.latch.getItem() != null;
      }
   }
}
