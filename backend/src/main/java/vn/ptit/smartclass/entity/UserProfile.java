package vn.ptit.smartclass.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Bang user_profiles - ho so sinh vien thuc hien de tai (muc 3.3.2.1) */
@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Column(name = "student_code", length = 20, nullable = false)
    private String studentCode;

    @Column(name = "bio", length = 500)
    private String bio;

    @Column(length = 100)
    private String major;

    @Column(length = 30)
    private String classCode;

    @Column(length = 120)
    private String email;

    @Column(length = 120)
    private String location;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "github_url")
    private String githubUrl;

    @Column(name = "figma_url")
    private String figmaUrl;

    @Column(name = "api_docs_url")
    private String apiDocsUrl;

    @Column(name = "report_pdf_url")
    private String reportPdfUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }

    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }

    public String getFigmaUrl() { return figmaUrl; }
    public void setFigmaUrl(String figmaUrl) { this.figmaUrl = figmaUrl; }

    public String getApiDocsUrl() { return apiDocsUrl; }
    public void setApiDocsUrl(String apiDocsUrl) { this.apiDocsUrl = apiDocsUrl; }

    public String getReportPdfUrl() { return reportPdfUrl; }
    public void setReportPdfUrl(String reportPdfUrl) { this.reportPdfUrl = reportPdfUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
