package com.gdut.rpcstudy.demo.framework.protocol.netty.asyn;

import com.gdut.rpcstudy.demo.framework.ProxyFactory;
import com.gdut.rpcstudy.demo.framework.serialize.tranobject.RpcRequest;
import com.gdut.rpcstudy.demo.framework.serialize.tranobject.RpcResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author: lele
 * @date: 2019/11/21 上午10:54
 * 实现异步返回
 */
public class RpcFuture implements Future<Object> {

    private RpcResponse rpcResponse;

    private RpcRequest rpcRequest;

    //自定义同步器，这里只是用来改变状态
    private Sync sync;

    //回调函数集合
    private List<IAsynCallBack> callBackList=new ArrayList<>();

    private ReentrantLock lock=new ReentrantLock();

    public RpcFuture(RpcRequest rpcRequest) {
        this.rpcRequest = rpcRequest;
        this.sync = new Sync();
    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    //返回状态是否改变了
    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    public void done(RpcResponse response) {
        this.rpcResponse = response;
        sync.tryRelease(1);
        //执行callback
        invokeCallbacks();

    }



    @Override
    public Object get() throws InterruptedException, ExecutionException {
        //自选等待结果
        sync.acquire(-1);
        return this.rpcResponse.getResult() != null ? rpcResponse :
                rpcResponse.getError() != null ? rpcResponse.getError() : null;

    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      //超时获取
        boolean success = sync.tryAcquireNanos(-1, unit.toNanos(timeout));
        return get(success);
    }

    private Object get(boolean success) {
        if (success) {
            return this.rpcResponse.getResult() != null ? rpcResponse : rpcResponse.getError() != null ? rpcResponse.getError() : null;
        } else {
            throw new RuntimeException("超时:requestID" + rpcRequest.getRequestId() +
                    " method:" + rpcRequest.getMethodName() + " interface:" + rpcRequest.getInterfaceName()
            );
        }
    }

    private void invokeCallbacks() {
        //锁定
        lock.lock();
        try {
            for (final IAsynCallBack callback : callBackList) {
                runCallback(callback);
            }
        } finally {
            lock.unlock();
        }
    }
    //执行回调
    private void runCallback(final IAsynCallBack callback) {
        final RpcResponse res = this.rpcResponse;
        ProxyFactory.submit(new Runnable() {
            @Override
            public void run() {
                if (res.getError()!=null) {
                    callback.success(res.getResult());
                } else {
                    callback.error(new RuntimeException("Response error"+res.getError()));
                }
            }
        });
    }

    //添加回调
    public RpcFuture addCallback(IAsynCallBack callback) {
        lock.lock();
        try {
            if (isDone()) {
                //如果在完成后添加的，则直接执行
                runCallback(callback);
            } else {
                //否则加入回调链进行处理
                this.callBackList.add(callback);
            }
        } finally {
            lock.unlock();
        }
        return this;
    }

    /**
     * 继承同步器，这里只是用来自旋改变状态，根据state来实现，state初始为0
     */
    static class Sync extends AbstractQueuedSynchronizer {
        /**
         * 尝试获取锁,如果获取不了，加入同步队列，阻塞自己，只由同步队列的头自旋获取锁
         * 当状态为1，即有结果返回时可以获取锁进行后续操作,设置result
         *这里只有一个节点，会不断自选尝试获取锁
         * @param arg
         * @return
         */
        @Override
        protected boolean tryAcquire(int arg) {
            return getState() == 1;
        }

        /**
         * 用于远端有返回时，设置状态变更
         * 从头唤醒同步队列的队头下一个等待的节点，如果下一个节点为空，则从队尾唤醒
         * @param arg
         * @return
         */
        @Override
        protected boolean tryRelease(int arg) {
            //把状态设置为1，给tryAcquire获取锁进行操作
            return getState() == 0 ? compareAndSetState(0, 1) : true;
        }

        public boolean isDone() {
            return getState() == 1;
        }
    }

}
