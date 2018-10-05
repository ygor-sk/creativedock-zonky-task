package sk.ygor.creativedock.zonky;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Loan {

    private final long id;
    private final String name;
    private final String rating;
    private final BigDecimal amount;

    public Loan(long id, String name, String rating, BigDecimal amount) {
        this.id = id;
        this.name = name;
        this.rating = rating;
        this.amount = amount;
    }

    public long getId() {
        return id;
    }

    public String getRating() {
        return rating;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "Loan{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", rating='" + rating + '\'' +
                ", amount=" + amount +
                '}';
    }
}
