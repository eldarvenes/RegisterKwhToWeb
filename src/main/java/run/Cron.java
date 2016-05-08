package run;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.RegisterKwhToWeb;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

public class Cron {

    final static Logger logger = LoggerFactory.getLogger(Cron.class);

    public static void main(String[] args) {

        logger.info("Entering application");


        Scheduler scheduler = null;
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();

            JobDetail saveReportToWeb = newJob(RegisterKwhToWeb.class).withIdentity("saveReportToWeb")
                    .build();

            //Registrer kwh total til kundeweb. Ved midnatt.
            Trigger trigger_saveReportToWeb = newTrigger()
                    .withIdentity("trigger_saveReportToWeb")
                    .startNow()
                    .withSchedule(cronSchedule("* 1 0 * * ?"))
                    .build();

            scheduler.start();
            scheduler.scheduleJob(saveReportToWeb, trigger_saveReportToWeb);

        } catch (SchedulerException e) {
            e.printStackTrace();
        }

    }
}
