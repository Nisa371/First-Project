package com.marketplace.training;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component @RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.training.seed-catalog",havingValue="true",matchIfMissing=true)
public class TrainingCatalog implements ApplicationRunner {
 private final TrainingProgramRepository programs;
 @Override @Transactional public void run(ApplicationArguments args) {
  for(var row:new String[][]{{"Workplace safety & communication","Shapla Skills Studio (fictional)","A four-week practical course covering safe work, communication and interview preparation. Ask your evaluator about the next step; this showcase does not enroll you automatically."},{"Digital foundations lab","Meghna Learning Lab (fictional)","Guided practice in problem solving, digital tools and building a small portfolio. A six-week learning pathway for developing confidence."}}) {
   if(programs.findAll().stream().anyMatch(p->p.getTitle().equals(row[0]))) continue;
   var p=new TrainingProgram();p.setTitle(row[0]);p.setProviderName(row[1]);p.setDescription(row[2]);programs.save(p);
  }
 }
}
