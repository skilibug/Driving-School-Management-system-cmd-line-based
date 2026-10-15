package ug.ac.vu.g13.fees;

import java.time.LocalDate;
import java.util.Comparator;

import ug.ac.vu.g13.core.BaseService;
import ug.ac.vu.g13.core.Fields;
import ug.ac.vu.g13.core.InvalidDataException;
import ug.ac.vu.g13.courses.Enrollment;
import ug.ac.vu.g13.courses.EnrollmentService;
import ug.ac.vu.g13.students.Student;

/**
 * Manages fee invoices.
 *
 * @author Darwin
 */
public class InvoiceService extends BaseService<Invoice> {
    public static final Comparator<Invoice> BY_BALANCE_DESC = Comparator.comparingDouble(Invoice::getBalance).reversed()
            .thenComparing(Invoice::getId);
    public static final Comparator<Invoice> BY_DUE_DATE = Comparator.comparing(Invoice::getDueDate).thenComparing(Invoice::getId);

    private final EnrollmentService enrollments;

    public InvoiceService(EnrollmentService enrollments) {
        super("invoices.txt", "Invoice");
        this.enrollments = enrollments;
        load();
    }

    @Override
    protected Invoice fromFileLine(String line) {
        String[] f = Fields.split(line, 5);
        return new Invoice(f[0], enrollments.require(f[1]), Double.parseDouble(f[2]), LocalDate.parse(f[3]),
                LocalDate.parse(f[4]));
    }

    /** The amount due is the fee of the enrolled course. */
    public Invoice issue(Enrollment enrollment, LocalDate dueDate) {
        if (anyMatch(i -> i.getEnrollment().equals(enrollment))) {
            throw new InvalidDataException("An invoice already exists for enrolment " + enrollment.getId() + ".");
        }
        Invoice inv = new Invoice(nextId('N'), enrollment, enrollment.getCourse().getFee(), LocalDate.now(), dueDate);
        add(inv);
        return inv;
    }

    /** True if at least one invoice of the student is paid for the given fraction (0.5 = half). */
    public boolean hasPaidAtLeast(Student student, double fraction) {
        return anyMatch(i -> i.getEnrollment().getStudent().equals(student)
                && i.getAmountPaid() + 0.0001 >= i.getTotalDue() * fraction);
    }
}
