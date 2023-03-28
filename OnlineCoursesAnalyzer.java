import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OnlineCoursesAnalyzer {

  List<Course> courses = new ArrayList<>();

  public OnlineCoursesAnalyzer(String datasetPath) {
    BufferedReader br = null;
    String line;
    try {
      br = new BufferedReader(new FileReader(datasetPath, StandardCharsets.UTF_8));
      br.readLine();
      while ((line = br.readLine()) != null) {
        String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
        Course course = new Course(info[0], info[1], new Date(info[2]), info[3], info[4], info[5],
            Integer.parseInt(info[6]), Integer.parseInt(info[7]), Integer.parseInt(info[8]),
            Integer.parseInt(info[9]), Integer.parseInt(info[10]), Double.parseDouble(info[11]),
            Double.parseDouble(info[12]), Double.parseDouble(info[13]),
            Double.parseDouble(info[14]),
            Double.parseDouble(info[15]), Double.parseDouble(info[16]),
            Double.parseDouble(info[17]),
            Double.parseDouble(info[18]), Double.parseDouble(info[19]),
            Double.parseDouble(info[20]),
            Double.parseDouble(info[21]), Double.parseDouble(info[22]));
        courses.add(course);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  //1
  public Map<String, Integer> getPtcpCountByInst() {
    return (HashMap<String, Integer>) this.courses.stream().sorted(
        (o1, o2) -> o2.institution.compareTo(o1.institution)).collect(
        Collectors.groupingBy(Course::getInstitution,
            Collectors.summingInt(Course::getParticipants)));
  }

  //2
  public Map<String, Integer> getPtcpCountByInstAndSubject() {
    HashMap<String, Integer> hashMap = (HashMap<String, Integer>) this.courses.stream()
        .collect(
            Collectors.groupingBy(
                course -> course.getInstitution() + "-" + course.getSubject(),
                Collectors.summingInt(Course::getParticipants))
        );

    return hashMap.entrySet()
        .stream()
        .sorted(
            ((o1, o2) -> {
              if (o2.getValue().compareTo(o1.getValue()) == 0) {
                return o2.getKey().compareTo(o1.getKey());
              } else {
                return o2.getValue().compareTo(o1.getValue());
              }
            })
        )
        .collect(Collectors
            .toMap(Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new));
  }

  //3
  public Map<String, List<List<String>>> getCourseListOfInstructor() {
    Map<String, List<Course>> hashmap = new HashMap<>();
    hashmap = this.courses.stream().collect(
        Collectors.groupingBy(
            Course::getInstructors
        )
    );
    HashMap<String, List<List<String>>> ans = new HashMap<>();

    hashmap.forEach((key, tmp) -> {
      for (Course nowCourse : tmp) {
        if (nowCourse.instructors.contains(",")) {
          String[] instructors = nowCourse.instructors.split(",");
          for (String nowInstructor : instructors) {
            nowInstructor = nowInstructor.strip();
            if (!ans.containsKey(nowInstructor)) {
              List<List<String>> value = new ArrayList<>();
              List<String> list1 = new ArrayList<>();
              List<String> list2 = new ArrayList<>();
              value.add(list1);
              value.add(list2);
              ans.put(nowInstructor, value);
              ans.get(nowInstructor).get(1).add(nowCourse.getTitle());
            } else {
              if (!ans.get(nowInstructor).get(1).contains(nowCourse.getTitle())) {
                ans.get(nowInstructor).get(1).add(nowCourse.getTitle());
              }
            }
          }
        } else {
          if (!ans.containsKey(nowCourse.getInstructors())) {
            List<List<String>> value = new ArrayList<>();
            List<String> list1 = new ArrayList<>();
            List<String> list2 = new ArrayList<>();
            value.add(list1);
            value.add(list2);
            ans.put(nowCourse.getInstructors(), value);
            ans.get(nowCourse.getInstructors()).get(0).add(nowCourse.getTitle());
          } else {
            if (!ans.get(nowCourse.getInstructors()).get(0).contains(nowCourse.getTitle())) {
              ans.get(nowCourse.getInstructors()).get(0).add(nowCourse.getTitle());
            }
          }
        }
      }
    });

    ans.forEach((key, tmp) -> {
      List<String> list1 = tmp.get(0);
      List<String> list2 = tmp.get(1);
      if (!list1.isEmpty()) {
        list1.sort(String::compareTo);
      }
      if (!list2.isEmpty()) {
        list2.sort(String::compareTo);
      }
    });
    return ans;
  }

  //4
  public List<String> getCourses(int topK, String by) {
    List<String> list = new ArrayList<>();
    List<Course> tmp = new ArrayList<>();
    if (by.equals("hours")) {
      tmp = this.courses.stream().sorted(
          ((o1, o2) -> (int) (o2.getTotalHours() - o1.getTotalHours()))
      ).collect(Collectors.toList());
    } else {
      tmp = this.courses.stream().sorted(
          ((o1, o2) -> (o2.getParticipants() - o1.getParticipants()))
      ).collect(Collectors.toList());
    }
    int cnt = 0;
    while (list.size() != topK) {
      String tmpAns = tmp.get(cnt++).getTitle();
      if (!list.contains(tmpAns)) {
        list.add(tmpAns);
      }
    }
    return list;
  }

  //5
  public List<String> searchCourses(String courseSubject, double percentAudited,
      double totalCourseHours) {
    List<String> ans = new ArrayList<>();
    this.courses.stream().filter(e ->
        e.getSubject().toLowerCase().contains(courseSubject.toLowerCase())
            && e.getPercentAudited() >= percentAudited && e.totalHours <= totalCourseHours
    ).map(Course::getTitle).sorted(String::compareTo).distinct().forEach(ans::add);
    return ans;
  }

  //6
  public List<String> recommendCourses(int age, int gender, int isBachelorOrHigher) {
    List<String> assist = new ArrayList<>();
    List<Course> oneCourse = new ArrayList<>();

    HashMap<String, Double> averageAges =
        (HashMap<String, Double>) this.courses.stream()
            .collect(Collectors.groupingBy(Course::getNumber,
                Collectors.averagingDouble(Course::getMedianAge)));






    HashMap<String, Double> averageMales =
        (HashMap<String, Double>) this.courses.stream()
            .collect(Collectors.groupingBy(Course::getNumber,
                Collectors.averagingDouble(Course::getPercentMale)));

    HashMap<String, Double> averageDegree =
        (HashMap<String, Double>) this.courses.stream()
            .collect(Collectors.groupingBy(Course::getNumber,
                Collectors.averagingDouble(Course::getPercentDegree)));

    this.courses.stream().sorted(
        (o1, o2) -> {
          if (o2.getLaunchDate().after(o1.getLaunchDate())) {
            return 1;
          } else if (o2.getLaunchDate().before(o1.getLaunchDate())) {
            return -1;
          } else {
            return 0;
          }
        }
    ).forEach(e -> {
      if (!assist.contains(e.getNumber())) {
        assist.add(e.getNumber());
        oneCourse.add(e);
      }
    });

    return oneCourse.stream().sorted(
        ((o1, o2) -> {
          double a = (
              cal(o1, age, gender, isBachelorOrHigher, averageAges, averageMales, averageDegree)
                  -
                  cal(o2, age, gender, isBachelorOrHigher, averageAges, averageMales,
                      averageDegree));
          if (a > 0) {
            return 1;
          } else if (a < 0) {
            return -1;
          } else {
            return o1.getTitle().compareTo(o2.getTitle());
          }
        })
    ).map(Course::getTitle).distinct().limit(10).toList();
  }

  public double cal(Course course, int age, int gender, int isBachelorOrHigher,
      HashMap<String, Double> hashMap, HashMap<String, Double> averageMale,
      HashMap<String, Double> averageDegree) {
    return power(age - hashMap.get(course.getNumber()))
        + power(100 * gender - averageMale.get(course.getNumber()))
        + power(100 * isBachelorOrHigher - averageDegree.get(course.getNumber()));
  }

  public double power(double item) {
    return item * item;
  }
}

class Course {

  String institution;
  String number;
  Date launchDate;
  String title;
  String instructors;
  String subject;
  int year;
  int honorCode;
  int participants;
  int audited;
  int certified;
  double percentAudited;
  double percentCertified;
  double percentCertified50;
  double percentVideo;
  double percentForum;
  double gradeHigherZero;
  double totalHours;
  double medianHoursCertification;
  double medianAge;
  double percentMale;
  double percentFemale;
  double percentDegree;

  public String getInstitution() {
    return institution;
  }

  public void setInstitution(String institution) {
    this.institution = institution;
  }

  public String getNumber() {
    return number;
  }

  public void setNumber(String number) {
    this.number = number;
  }

  public Date getLaunchDate() {
    return launchDate;
  }

  public void setLaunchDate(Date launchDate) {
    this.launchDate = launchDate;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getInstructors() {
    return instructors;
  }

  public void setInstructors(String instructors) {
    this.instructors = instructors;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public int getYear() {
    return year;
  }

  public void setYear(int year) {
    this.year = year;
  }

  public int getHonorCode() {
    return honorCode;
  }

  public void setHonorCode(int honorCode) {
    this.honorCode = honorCode;
  }

  public int getParticipants() {
    return participants;
  }

  public int getCertified() {
    return certified;
  }

  public void setCertified(int certified) {
    this.certified = certified;
  }

  public double getPercentAudited() {
    return percentAudited;
  }

  public void setPercentAudited(double percentAudited) {
    this.percentAudited = percentAudited;
  }

  public double getPercentCertified() {
    return percentCertified;
  }

  public void setPercentCertified(double percentCertified) {
    this.percentCertified = percentCertified;
  }

  public double getPercentCertified50() {
    return percentCertified50;
  }

  public void setPercentCertified50(double percentCertified50) {
    this.percentCertified50 = percentCertified50;
  }

  public double getPercentVideo() {
    return percentVideo;
  }

  public void setPercentVideo(double percentVideo) {
    this.percentVideo = percentVideo;
  }

  public double getPercentForum() {
    return percentForum;
  }

  public void setPercentForum(double percentForum) {
    this.percentForum = percentForum;
  }

  public double getGradeHigherZero() {
    return gradeHigherZero;
  }

  public void setGradeHigherZero(double gradeHigherZero) {
    this.gradeHigherZero = gradeHigherZero;
  }

  public double getTotalHours() {
    return totalHours;
  }

  public void setTotalHours(double totalHours) {
    this.totalHours = totalHours;
  }

  public double getMedianHoursCertification() {
    return medianHoursCertification;
  }

  public void setMedianHoursCertification(double medianHoursCertification) {
    this.medianHoursCertification = medianHoursCertification;
  }

  public double getMedianAge() {
    return medianAge;
  }

  public void setMedianAge(double medianAge) {
    this.medianAge = medianAge;
  }

  public double getPercentMale() {
    return percentMale;
  }

  public void setPercentMale(double percentMale) {
    this.percentMale = percentMale;
  }

  public double getPercentFemale() {
    return percentFemale;
  }

  public void setPercentFemale(double percentFemale) {
    this.percentFemale = percentFemale;
  }

  public double getPercentDegree() {
    return percentDegree;
  }

  public void setPercentDegree(double percentDegree) {
    this.percentDegree = percentDegree;
  }

  public void setParticipants(int participants) {
    this.participants = participants;
  }

  public int getAudited() {
    return audited;
  }

  public void setAudited(int audited) {
    this.audited = audited;
  }

  public Course(String institution, String number, Date launchDate,
      String title, String instructors, String subject,
      int year, int honorCode, int participants,
      int audited, int certified, double percentAudited,
      double percentCertified, double percentCertified50,
      double percentVideo, double percentForum, double gradeHigherZero,
      double totalHours, double medianHoursCertification,
      double medianAge, double percentMale, double percentFemale,
      double percentDegree) {
    this.institution = institution;
    this.number = number;
    this.launchDate = launchDate;
    if (title.startsWith("\"")) {
      title = title.substring(1);
    }
    if (title.endsWith("\"")) {
      title = title.substring(0, title.length() - 1);
    }
    this.title = title;
    if (instructors.startsWith("\"")) {
      instructors = instructors.substring(1);
    }
    if (instructors.endsWith("\"")) {
      instructors = instructors.substring(0, instructors.length() - 1);
    }
    this.instructors = instructors;
    if (subject.startsWith("\"")) {
      subject = subject.substring(1);
    }
    if (subject.endsWith("\"")) {
      subject = subject.substring(0, subject.length() - 1);
    }
    this.subject = subject;
    this.year = year;
    this.honorCode = honorCode;
    this.participants = participants;
    this.audited = audited;
    this.certified = certified;
    this.percentAudited = percentAudited;
    this.percentCertified = percentCertified;
    this.percentCertified50 = percentCertified50;
    this.percentVideo = percentVideo;
    this.percentForum = percentForum;
    this.gradeHigherZero = gradeHigherZero;
    this.totalHours = totalHours;
    this.medianHoursCertification = medianHoursCertification;
    this.medianAge = medianAge;
    this.percentMale = percentMale;
    this.percentFemale = percentFemale;
    this.percentDegree = percentDegree;
  }
}
