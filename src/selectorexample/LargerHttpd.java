package selectorexample;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;
import java.nio.channels.*;

public class LargerHttpd
{

    Selector clientSelector;

    public void run(int port, int threads) throws IOException
    {
        clientSelector = Selector.open();
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        InetSocketAddress sa = new InetSocketAddress(InetAddress
                .getLoopbackAddress(), port);
        ssc.socket().bind(sa);
        ssc.register(clientSelector, SelectionKey.OP_ACCEPT);

        Executor executor = Executors.newFixedThreadPool(threads);

        while (true)
        {
            try
            {
                while (clientSelector.select(100) == 0);
                Set<SelectionKey> readySet = clientSelector.selectedKeys();
                for (Iterator<SelectionKey> it = readySet.iterator();
                        it.hasNext();)
                {
                    final SelectionKey key = it.next();
                    it.remove();
                    if (key.isAcceptable())
                    {
                        acceptClient(ssc);
                    } 
                    else
                    {
                        key.interestOps(0);
                        executor.execute(new Runnable()
                        {
                            public void run()
                            {
                                try
                                {
                                    handleClient(key);
                                } 
                                catch (IOException e)
                                {
                                    System.out.println(e);
                                }
                            }
                        });
                    }
                }
            } 
            catch (IOException e)
            {
                System.out.println(e);
            }
        }
    }

    void acceptClient(ServerSocketChannel ssc) throws IOException
    {
        SocketChannel clientSocket = ssc.accept();
        clientSocket.configureBlocking(false);
        SelectionKey key = clientSocket.register(clientSelector,
                SelectionKey.OP_READ);
        HttpdConnection client = new HttpdConnection(clientSocket);
        key.attach(client);
    }

    void handleClient(SelectionKey key) throws IOException
    {
        HttpdConnection client = (HttpdConnection) key.attachment();
        if (key.isReadable())
        {
            client.read(key);
        } 
        else
        {
            client.write(key);
        }
        clientSelector.wakeup();
    }

    public static void main(String argv[]) throws IOException
    {
        //new LargerHttpd().run( Integer.parseInt(argv[0]), 3/*threads*/ );
        new LargerHttpd().run(1235, 3/*threads*/);
    }
}
