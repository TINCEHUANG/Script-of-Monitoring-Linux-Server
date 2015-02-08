
public class Sender implements Runnable
{
	private Connectionhandler ch;
	private long sleepTime;
	private ServerInfo si;
	private double lastRT = 0;
	
	public Sender(Connectionhandler ch, ServerInfo si, long sleepTime)
	{
		this.ch = ch;
		this.sleepTime = sleepTime;
		this.si = si;
	}

	@Override
	public void run()
	{
		while(true)
		{
			try
			{
				Thread.sleep(sleepTime);
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				lastRT = si.getResponseTime() / 1000000.0;
				int connections = si.getConnection();				
				if(connections == 0)
					lastRT = 0;
				String info = String.format("%.2f", lastRT) + " " + connections + " " + 
					String.format("%.2f", si.getCpuInfo()) + " " + 	si.getMemoryInfo();
				System.out.println("Sending: "+ info);
				ch.sendInfo(info);	
		}
	}
}
