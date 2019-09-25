package models;

 public enum AgentRecordPhase {
   START, END;
   
   private static final AgentRecordPhase byIndex[] = AgentRecordPhase.class.getEnumConstants();
   
   public static AgentRecordPhase byIndex(int index) {
     return byIndex[index];
   }

   public static AgentRecordPhase[] all() {
     return byIndex.clone();
   }

   public static int maxOrdinal() {
     return byIndex[byIndex.length - 1].ordinal();
   }
 }
