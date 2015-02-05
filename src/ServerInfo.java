import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class ServerInfo 
{
	private String MyIp;
	private HashMap<String, Long> messageBuffer;
	long responseTime;
	private int nConnections;

	public ServerInfo ()
	{
		responseTime = 0;
		nConnections = 0;
		messageBuffer = new HashMap<String, Long>();
		try
		{
			Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
			while(e.hasMoreElements())
			{
				NetworkInterface n = (NetworkInterface) e.nextElement();
				Enumeration<InetAddress> ee = n.getInetAddresses();
				while (ee.hasMoreElements())
				{
					InetAddress i = (InetAddress) ee.nextElement();
					System.out.println("interface: "+n.getDisplayName());
					if(n.getDisplayName().compareTo("eth0")==0)
					{
						MyIp = i.getHostAddress();
					}
					//System.out.println(i.getHostAddress());
				}
			}


			System.out.println("ip:" +MyIp);
		} catch (SocketException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public void startTcpdump()
	{
		long ns = 0;
		String src = null;
		String dst = null;
		while(true)
		{
			//System.out.println("STARTING TCPDUMP");
			String [] opts = null;
			String[] tcpdumpCmd = {"/usr/sbin/tcpdump","-l", "-nntt" ,"-i", "eth1"};
			try
			{
				Process p = new ProcessBuilder(tcpdumpCmd).start();
				BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String tcpdumpOut = null;
				while ((tcpdumpOut = in.readLine()) != null )
				{
					opts = tcpdumpOut.split(" ");
					if (opts.length > 9 && opts[1].compareTo("IP") == 0 && opts[5].compareTo("Flags") == 0)
					{
						try
						{
							ns = System.nanoTime();
							src = opts[2];
							dst = opts[4].substring(0, opts[4].length()-1);
							String flag = opts[6];
							String seq = opts[8];
							computeResponseTime(ns, src, dst, flag, seq);
						}
						catch(Exception e)
						{

						}
					}
				}
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void computeResponseTime(long ns, String src, String dst, String flag, String seq)
	{
		int dstPos = dst.lastIndexOf('.');
		int srcPos = src.lastIndexOf('.');
		String dstIp = dst.substring(0, dstPos);
		String srcIp = src.substring(0, srcPos);
		if (flag.contains("P"))
		{
			String[] seqs = seq.split(":");
			if (seqs.length == 2)
			{
				if ((dstIp.compareTo(MyIp) == 0) && (messageBuffer.get(src + ">" + dst + " seq " + seqs[1]) == null) )
				{
					messageBuffer.put(src + ">" + dst + " seq " + seqs[1], ns);
					//System.out.println(src + ">" + dst + " seq " + seqs[1] + " time: " + ts);
				}
			}
		}
		else
		{
				if (messageBuffer.get(dst + ">" + src + " seq " + seq) != null)
				{
                                this.responseTime = ns - messageBuffer.get(dst + ">" + src + " seq " + seq);
                                messageBuffer.remove(dst + ">" + src + " seq " + seq);
                                //System.out.println(src + ">" + dst + " seq " + seq + " time: " + ts);
                                //System.out.println("Response Time: " + this.responseTime);
				}
		}
	}
	
	public long getResponseTime()
	{
		return this.responseTime;
	}

	public void setResponseTime(long value)
	{
		this.responseTime = value;
	}

	public int getConnection()
	{
		String s;
		int connections = 0;
		String[] cmd = {
				"/bin/sh",
				"-c",
				"netstat -an | grep "+MyIp+" | grep -c ESTABLISHED"
		};
		Process p;
		try
		{
			p = Runtime.getRuntime().exec(cmd);
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((s = br.readLine()) != null)
			{
				connections = Integer.parseInt(s);
			}
			p.waitFor();
			System.out.println ("exit: " + p.exitValue());
			p.destroy();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return connections;
	}

	public double getCpuInfo()
	{
		String s;
		double cpuUsage = 0;
		Process p;
		try
		{
			p = Runtime.getRuntime().exec("mpstat 2 5");
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((s = br.readLine()) != null)
			{
				if(s.contains("Average") || s.contains("平均时间"))
				{
					String [] aux = s.split(" ");
					String idle = null;
					for(int i = aux.length ; i > 0; i-- )
					{
						try
						{
							if(Character.isDigit(aux[i].charAt(0)))
							{
								idle = aux[i];
								i = 0;
							}
						}
						catch (Exception e)
						{
							continue;
						}
					}
					cpuUsage = 100 - Double.parseDouble(idle);
					//System.out.println("CPU usage: "+cpuUsage);
				}
			}
			p.waitFor();
			p.destroy();

			//  System.out.println("CPU usage: "+cpuUsage.doubleValue());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return cpuUsage;
	}
	
	public double getMemoryInfo()
	{
		String s;
		double memUsage = 0;//memUsage = memUsed / memAll * 100
        Double memAll = 0.0,memUsed = 0.0;
		String cmd = "free -m";
		Process p;
		try {
			p = Runtime.getRuntime().exec(cmd);
			BufferedReader br = new BufferedReader(
					new InputStreamReader(p.getInputStream()));
			while ((s = br.readLine()) != null){
				if(s.contains("Mem:")){
					String [] aux = s.split(" ");
					
					for(int i = 0 ; i<aux.length; i++ ){
						try{
							if(Character.isDigit(aux[i].charAt(0))){
								memAll = Double.parseDouble((aux[i]));
								i = aux.length;
								//System.out.println("memAll:"+ memAll);      
							}
						}catch (Exception e){
							continue;
						}
					}
				}
				if(s.contains("buffer")){
					String [] aux = s.split(" ");
					
					for(int i = 0 ; i<aux.length; i++ ){
						try{
							if(Character.isDigit(aux[i].charAt(0))){
								memUsed = Double.parseDouble(aux[i]);
								i = aux.length;   
								//System.out.println("memUsed:"+ memUsed);
							}
						}catch (Exception e){
							continue;
						}

					}
				}
			}
			p.waitFor();
			p.destroy();

			//  System.out.println("CPU usage: "+cpuUsage.doubleValue());
		} catch (Exception e) {
			e.printStackTrace();
		}
		DecimalFormat dcmFmt = new DecimalFormat("0.00");
        memUsage = memUsed / memAll * 100;
        memUsage = Double.parseDouble(dcmFmt.format((memUsage)));//normalization like CPUusage
        //System.out.println("memUsage:" + memUsage);
	
		return memUsage;
	}
}


