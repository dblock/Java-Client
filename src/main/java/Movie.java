public class Movie {
    private String Director;
    private String Title;
    private Integer Year;

    public Movie() {

    }

    public Movie(String Director, String Title, Integer Year) {
        this.Director = Director;
        this.Title = Title;
        this.Year = Year;
    }

    public String getDirector() {
        return Director;
    }

    public void setDirector(String Director) {
        this.Director = Director;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String Title) {
        this.Title = Title;
    }

    public void setYear(Integer Year) {
        this.Year = Year;
    }

    public Integer getYear() {
        return Year;
    }

    @Override
    public String toString() {
        return String.format("Movie{Director='%s', Title='%s', Year=%s}", Director, Title, Year);
    }
}