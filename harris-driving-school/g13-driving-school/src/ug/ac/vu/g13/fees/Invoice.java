package ug.ac.vu.g13.fees;

import java.time.LocalDate;

import ug.ac.vu.g13.core.BaseRecord;
import ug.ac.vu.g13.core.Fields;
import ug.ac.vu.g13.core.InvalidDataException;
import ug.ac.vu.g13.core.Payable;
import ug.ac.vu.g13.core.Validate;
import ug.ac.vu.g13.courses.Enrollment;

/**
 * A fee invoice for one enrolment. Implements the shared Payable interface.
 * Links the Fees module to Courses and Students by object reference.
 *
 * @author Darwin 
 */
public class Invoice extends BaseRecord implements Payable {
    private final Enrollment enrollment;
    private final double totalDue;
    private final LocalDate issuedOn;
    private LocalDate dueDate;
    private double amountPaid;

    public Invoice(String id, Enrollment enrollment, double totalDue, LocalDate issuedOn, LocalDate dueDate) {
        super(id);
        this.enrollment = Validate.notNull(enrollment, "Enrolment");
        this.totalDue = Validate.doubleRange(totalDue, "Amount due (UGX)", 1000, 10_000_000);
        this.issuedOn = Validate.date(issuedOn, "Issue date", LocalDate.of(2000, 1, 1), LocalDate.now().plusDays(1));
        setDueDate(dueDate);
    }

    public Enrollment getEnrollment() {
        return enrollment;
    }

    @Override
    public double getTotalDue() {
        return totalDue;
    }

    @Override
    public double getAmountPaid() {
        return amountPaid;
    }

    public LocalDate getIssuedOn() {
        return issuedOn;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        Validate.notNull(dueDate, "Due date");
        if (dueDate.isBefore(issuedOn) || dueDate.isAfter(issuedOn.plusYears(2))) {
            throw new InvalidDataException("Due date must be between the issue date and two years after it.");
        }
        this.dueDate = dueDate;
    }

    void addPaid(double amount) {
        this.amountPaid += amount;
    }

    void reducePaid(double amount) {
        this.amountPaid = Math.max(0, this.amountPaid - amount);
    }

    public boolean isOverdue() {
        return !isSettled() && dueDate.isBefore(LocalDate.now());
    }

    @Override
    public String describe() {
        return String.format("%s | Invoice | %-20s | %s | due %s | paid %s | balance %s%s",
                getId(), enrollment.getStudent().getFullName(), enrollment.getCourse().getName(), dueDate,
                Validate.money(amountPaid), Validate.money(getBalance()),
                isSettled() ? " | SETTLED" : (isOverdue() ? " | OVERDUE" : ""));
    }

    @Override
    public String toFileLine() {
        return Fields.join(getId(), enrollment.getId(), totalDue, issuedOn, dueDate);
    }
}
