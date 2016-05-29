import java.io.*;
import java.util.*;
import java.util.Date;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class test {

	private static String getTime() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date); //2014/08/06 15:59:48
	}

	public static void main(String [] args) {
		System.out.println("hello");
		String curr = getTime();
		System.out.println("current time = " +curr);
		// System.out.println("time is " + curr.split(" ")[1]);

		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

		Calendar cal = Calendar.getInstance();
		//System.out.println(dateFormat.format(cal.getTime()));

		System.out.println(cal);
		//cal.setTime(dateInstance);
		cal.add(Calendar.DATE, -1);

		Date dateBefore30Days = cal.getTime();
		System.out.println(dateFormat.format(dateBefore30Days));

	}
}