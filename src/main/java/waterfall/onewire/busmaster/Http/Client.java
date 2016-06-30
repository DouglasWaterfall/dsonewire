package waterfall.onewire.busmaster.Http;

import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.*;

/**
 * Created by dwaterfa on 2/15/16.
 */
public class Client implements BusMaster {
    private final String USER_AGENT = "waterfall waterfall.onewire.busmaster.HA7S.HTTP;1.0";

    Client(String url, String portName) {
        //       this.url = url;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public long getCurrentTimeMillis() {
        return 0;
    }

    @Override
    public StartBusCmd queryStartBusCmd(Logger optLogger) {
        return null;
    }

    @Override
    public StopBusCmd queryStopBusCmd(Logger optLogger) {
        return null;
    }

    @Override
    public SearchBusCmd querySearchBusCmd(Logger optLogger) {
        return null;
    }

    @Override
    public SearchBusCmd querySearchBusByFamilyCmd(short familyCode, Logger optLogger) {
        return null;
    }

    @Override
    public SearchBusCmd querySearchBusByAlarmCmd(Logger optLogger) {
        return null;
    }

    @Override
    public ConvertTCmd queryConvertTCmd(DSAddress dsAddr, Logger optLogger) {
        return null;
    }

    @Override
    public ReadPowerSupplyCmd queryReadPowerSupplyCmd(DSAddress dsAddr, Logger optLogger) {
        return null;
    }

    @Override
    public ReadScratchpadCmd queryReadScratchpadCmd(DSAddress dsAddr, short requestByteCount, Logger optLogger) {
        return null;
    }
}

/*
    @Override
    public StartResult start() {
        if (connection == null) {
            try {
                URL t_url = new URL(url);


                //
                // Need to create a request which will start the remote serial device.
                // the request has to have the serial device we want to talk to.
                //
                // The read request has a maximum size based on the rBuf length.
                // It would be nice to return the log information about what was read rather than actually log it.
                // That would make logging specific devices easier.
                // the maximum request/response size is probably interesting, but we do not expect very much
                // information to flow across the wire.
                // It seems tempting to see if we could make the request an object, but we need a response anyway.

                // the device should be the one to be returning the current time millis since it relates to the
                // returned information from the read.



                connection = (HttpURLConnection) t_url.openConnection();
            }
            catch(MalformedURLException e) {
                // bad url
                return StartResult.SR_NoPortName
            } catch (IOException e) {
                // openConnection faied
                return StartResult.SR_InternalException;
            }
        }

        return StartResult.SR_Success;
    }

    @Override
    public ReadResult writeReadTilCR(byte[] wBuf, int wOffset, int wCount, byte[] rBuf, int rOffset, int rTimeoutMSec) {
        if (connection == null) {
            return new myBadReadResult(ReadResult.ErrorCode.RR_NotStarted);
        }

        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "application/json");

            ClientRequest req = new ClientRequest(wBuf, wOffset, wCount, rTimeoutMSec);

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(req);

            OutputStream os = connection.getOutputStream();
            os.write(json.getBytes());
            os.flush();

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return new myBadReadResult(ReadResult.ErrorCode.RR_Exception);
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((connection.getInputStream())));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();

            ServerResponse resp = mapper.readValue(response.toString(), new TypeReference<ServerResponse>(){});
            if (resp == null) {
                return new myBadReadResult(ReadResult.ErrorCode.RR_InternalError;
            }
            if (resp.error != ReadResult.ErrorCode.RR_Success) {
                return new myBadReadResult(resp.error);
            }

            int readCount = (resp.rbuf != null) ? resp.rbuf.length : 0;
            for (int i = 0; i < readCount; i++) {
                rBuf[rOffset + i] = resp.rbuf[i];
            }

            return new myGoodReadResult(readCount, resp.postWriteCTM);
        }
        catch (ProtocolException e) {

        }
        catch (java.io.IOException e) {

        }

    }

    @Override
    public StopResult stop() {
        if (connection != null) {
            connection.disconnect();
            connection = null;
        }

        return StopResult.SR_Success;
    }

    private class myBadReadResult extends ReadResult {
        myBadReadResult(ErrorCode e) {
            this.error = e;
        }
    }

    private class myGoodReadResult extends ReadResult {
        myGoodReadResult(int readCount, Long postWriteCTM) {
            this.error = ErrorCode.RR_Success;
            this.readCount = readCount;
            this.postWriteCTM = postWriteCTM;
        }
    }

    private String url = null;
    private HttpURLConnection connection = null;
*/

    /*
    String url = "http://www.google.com/search?q=mkyong";

    URL obj = new URL(url);
    HttpURLConnection con = (HttpURLConnection) obj.openConnection();

// optional default is GET
con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", USER_AGENT);

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
        new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
        }
        in.close();


        import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonMapExample {

	public static void main(String[] args) {

		try {

			ObjectMapper mapper = new ObjectMapper();
			String json = "{\"name\":\"mkyong\", \"age\":29}";

			Map<String, Object> map = new HashMap<String, Object>();

			// convert JSON string to Map
			map = mapper.readValue(json, new TypeReference<Map<String, String>>(){});

			System.out.println(map);

		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
*/