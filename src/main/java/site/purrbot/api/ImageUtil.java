package site.purrbot.api;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import site.purrbot.api.endpoints.Quote;
import site.purrbot.api.endpoints.Status;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class ImageUtil {

    ImageUtil(){}

    private final OkHttpClient CLIENT = new OkHttpClient();

    private byte[] getAvatar(String url) throws IOException{
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "PurrBot-API")
                .build();

        try(Response response = CLIENT.newCall(request).execute()){
            if(!response.isSuccessful())
                throw new IOException(String.format(
                        "Couldn't get image! The server responded with %d (%s)",
                        response.code(),
                        response.message()
                ));
    
            ResponseBody body = response.body();
            if(body == null)
                throw new NullPointerException("Received empty body.");

            return body.bytes();
        }
    }

    private BufferedImage getAvatar(String url, int size) throws IOException{
        InputStream is = new ByteArrayInputStream(getAvatar(url));

        BufferedImage source = ImageIO.read(is);
        BufferedImage avatar = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        Graphics2D img = avatar.createGraphics();
        img.setClip(new Ellipse2D.Float(0, 0, size, size));
        img.drawImage(source, 0, 0, size, size, null);
        img.dispose();

        return avatar;
    }

    byte[] getQuote(Quote quote) throws IOException{
        BufferedImage ava = getAvatar(quote.getAvatar(), 217);
        
        String[] text = quote.getMessage().split("\\s");
        
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 55);
        BufferedImage image = new BufferedImage(1920, 300, BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D img = image.createGraphics();
        Color color = getColor(quote.getNameColor());
        
        String msg = "";
        List<String> messages = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        int lines = 1;
        
        for(String s : text){
            if(s.contains("\n")){
                if(s.endsWith("\n")){
                    s = s.replace("\n", "");
                    if(img.getFontMetrics(font).stringWidth(msg + " " + s) >= 1500){
                        messages.add(msg);
                        builder = new StringBuilder();
                        lines++;
                    }
                    
                    builder.append(s);
                    msg = builder.toString();
                    messages.add(msg);
                    
                    builder = new StringBuilder();
                    lines++;
                }else{
                    String before = s.split("\n")[0];
                    String after = s.split("\n")[1];
                    
                    if(img.getFontMetrics(font).stringWidth(msg + " " + before) >= 1500){
                        messages.add(msg);
                        builder = new StringBuilder();
                        lines++;
                    }
                    
                    builder.append(before);
                    msg = builder.toString();
                    messages.add(msg);
                    
                    msg = "";
                    builder = new StringBuilder();
                    
                    lines++;
                    
                    if(after.isEmpty())
                        continue;
                    
                    builder.append(after).append(" ");
                    msg = builder.toString();
                }
                continue;
            }
            
            if(img.getFontMetrics(font).stringWidth(msg + " " + s) >= 1500){
                messages.add(msg);
                builder = new StringBuilder();
                lines++;
            }
            
            builder.append(s).append(" ");
            msg = builder.toString();
        }
        
        messages.add(msg);
        
        int height = 110 + (lines * 60);
        
        image = resize(image, Math.max(height, image.getHeight()));
        img = image.createGraphics();
        
        img.setColor(new Color(0x36393F));
        img.fillRect(0, 0, image.getWidth(), image.getHeight());
        
        img.drawImage(ava, 10, 10, null);
        
        Font fontName = new Font(Font.SANS_SERIF, Font.BOLD, 60);
        Font fontDate = new Font(Font.SANS_SERIF, Font.PLAIN, 30);
        
        if(color == null)
            color = Color.WHITE;
        
        img.setColor(color);
        img.setFont(fontName);
        
        img.drawString(quote.getUsername(), 290, 65);
        
        long timestamp = Long.parseLong(quote.getTimestamp());
        
        Date date = new Date(timestamp);
        SimpleDateFormat df = new SimpleDateFormat(quote.getDateFormat());
        String time = df.format(date);
        
        img.setColor(new Color(0x67717A));
        img.setFont(fontDate);
        
        int posX = 300 + img.getFontMetrics(fontName).stringWidth(quote.getUsername());
        
        img.drawString(time, posX, 65);
        
        img.setColor(Color.WHITE);
        img.setFont(font);
        
        int posY = 150;
        for(String s : messages){
            img.drawString(s, 290, posY);
            posY += 60;
        }
        
        img.dispose();
        
        byte[] raw;
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream()){
            ImageIO.write(image, "png", baos);
            
            baos.flush();
            raw = baos.toByteArray();
        }
        
        return raw;
    }

    byte[] getStatus(Status status) throws IOException{
        
        BufferedImage ava = getAvatar(status.getAvatar(), 950);
        String type;
        switch(status.getStatus().toLowerCase()){
            case "online":
                type = status.isMobile() ? "online_mobile" : "online";
                break;
            
            case "idle":
                type = status.isMobile() ? "idle_mobile" : "idle";
                break;
            
            case "do_not_disturb":
            case "dnd":
                type = status.isMobile() ? "dnd_mobile" : "dnd";
                break;
            
            case "offline":
            default:
                type = "offline";
        }
    
        BufferedImage statusImg = ImageIO.read(new File("img/status/" + type + ".png"));
        Graphics2D img = ava.createGraphics();
        
        int x = ava.getWidth() - statusImg.getWidth();
        int y = ava.getHeight() - statusImg.getHeight();
        
        img.drawImage(statusImg, x, y, null);
        img.dispose();
        
        byte[] raw;
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream()){
            ImageIO.write(ava, "png", baos);
            
            baos.flush();
            raw = baos.toByteArray();
        }
        
        return raw;
    }

    private BufferedImage resize(BufferedImage image, int size){
        BufferedImage output = new BufferedImage(image.getWidth(), size, image.getType());

        Graphics2D img = output.createGraphics();
        img.drawImage(image, 0, 0, image.getWidth(), size, null);
        img.dispose();

        return output;
    }

    private Color getColor(String input){
        Color color;
        if(!input.toLowerCase().startsWith("hex:") && !input.toLowerCase().startsWith("rgb:")){
            try{
                color = new Color(Integer.parseInt(input));
            }catch(Exception ex){
                color = null;
            }

            return color;
        }

        String type = input.toLowerCase().split(":")[0];
        String value = input.toLowerCase().split(":")[1];

        switch(type){
            case "hex":
                try{
                    color = Color.decode(value.startsWith("#") ? value : "#" + value);
                }catch(Exception ex){
                    color = null;
                }
                break;

            case "rgb":
                String[] rgb = value.replace(" ", "").split(",");

                if(rgb.length < 3)
                    return null;

                try{
                    color = new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
                }catch(Exception ex){
                    color = null;
                }
                break;

            default:
                color = null;
        }

        return color;
    }
}
