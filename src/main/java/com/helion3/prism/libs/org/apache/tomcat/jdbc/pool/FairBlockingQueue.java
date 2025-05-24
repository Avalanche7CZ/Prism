package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool;

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
import java.util.concurrent.locks.ReentrantLock;

public class FairBlockingQueue implements BlockingQueue {
   static final boolean isLinux = "Linux".equals(System.getProperty("os.name")) && !Boolean.getBoolean(FairBlockingQueue.class.getName() + ".ignoreOS");
   final ReentrantLock lock = new ReentrantLock(false);
   final LinkedList items = new LinkedList();
   final LinkedList waiters = new LinkedList();

   public boolean offer(Object e) {
      ReentrantLock lock = this.lock;
      lock.lock();
      ExchangeCountDownLatch c = null;

      try {
         if (this.waiters.size() > 0) {
            c = (ExchangeCountDownLatch)this.waiters.poll();
            c.setItem(e);
            if (isLinux) {
               c.countDown();
            }
         } else {
            this.items.addFirst(e);
         }
      } finally {
         lock.unlock();
      }

      if (!isLinux && c != null) {
         c.countDown();
      }

      return true;
   }

   public boolean offer(Object e, long timeout, TimeUnit unit) throws InterruptedException {
      return this.offer(e);
   }

   public Object poll(long timeout, TimeUnit unit) throws InterruptedException {
      Object result = null;
      ReentrantLock lock = this.lock;
      boolean error = true;
      lock.lock();

      try {
         result = this.items.poll();
         if (result == null && timeout > 0L) {
            ExchangeCountDownLatch c = new ExchangeCountDownLatch(1);
            this.waiters.addLast(c);
            lock.unlock();
            if (!c.await(timeout, unit)) {
               lock.lock();
               this.waiters.remove(c);
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
      Future result = null;
      ReentrantLock lock = this.lock;
      boolean error = true;
      lock.lock();

      try {
         Object item = this.items.poll();
         if (item == null) {
            ExchangeCountDownLatch c = new ExchangeCountDownLatch(1);
            this.waiters.addLast(c);
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
      ReentrantLock lock = this.lock;
      lock.lock();

      boolean var3;
      try {
         var3 = this.items.remove(e);
      } finally {
         lock.unlock();
      }

      return var3;
   }

   public int size() {
      return this.items.size();
   }

   public Iterator iterator() {
      return new FairIterator();
   }

   public Object poll() {
      ReentrantLock lock = this.lock;
      lock.lock();

      Object var2;
      try {
         var2 = this.items.poll();
      } finally {
         lock.unlock();
      }

      return var2;
   }

   public boolean contains(Object e) {
      ReentrantLock lock = this.lock;
      lock.lock();

      boolean var3;
      try {
         var3 = this.items.contains(e);
      } finally {
         lock.unlock();
      }

      return var3;
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
         ReentrantLock lock = FairBlockingQueue.this.lock;
         lock.lock();

         try {
            this.elements = (Object[])(new Object[FairBlockingQueue.this.items.size()]);
            FairBlockingQueue.this.items.toArray(this.elements);
            this.index = 0;
         } finally {
            lock.unlock();
         }

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
         ReentrantLock lock = FairBlockingQueue.this.lock;
         lock.lock();

         try {
            if (this.element != null) {
               FairBlockingQueue.this.items.remove(this.element);
            }
         } finally {
            lock.unlock();
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
