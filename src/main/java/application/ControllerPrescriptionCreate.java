package application;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import view.*;

@Controller
public class ControllerPrescriptionCreate {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	/*
	 * Doctor requests blank form for new prescription.
	 */
	@GetMapping("/prescription/new")
	public String getPrescriptionForm(Model model) {
		model.addAttribute("prescription", new PrescriptionView());
		return "prescription_create";
	}

	// process data entered on prescription_create form
	@PostMapping("/prescription")
	public String createPrescription(PrescriptionView p, Model model) {

    System.out.println("createPrescription " + p);
    try (Connection conn = getConnection();) {
      /*
       * valid doctor name and id
       */
      //TODO
      PreparedStatement psDoctor = conn.prepareStatement(
          "SELECT id FROM doctor WHERE last_name = ? and first_name = ? and id = ?");
      psDoctor.setString(1, p.getDoctorLastName());
      psDoctor.setString(2, p.getDoctorFirstName());
      psDoctor.setInt(3, p.getDoctor_id());
      ResultSet rsDoctor = psDoctor.executeQuery();
      if (!rsDoctor.next()) {
        model.addAttribute("message", "Doctor not found. Check if "
            + "doctor id, first name and last name are correct.");
        model.addAttribute("prescription", p);
        return "prescription_create";
      }
      int doctorId = rsDoctor.getInt("id");
      /*
       * valid patient name and id
       */
      //TODO
      PreparedStatement psPatient = conn.prepareStatement(
          "SELECT id FROM patient WHERE last_name = ? and first_name = ? and id = ?");
      psPatient.setString(1, p.getPatientLastName());
      psPatient.setString(2, p.getPatientFirstName());
      psPatient.setInt(3, p.getPatient_id());
      ResultSet rsPatient = psPatient.executeQuery();
      if (!rsPatient.next()) {
        model.addAttribute("message", "Patient not found. Check if"
            + " patient id, first name and last name are correct.");
        model.addAttribute("prescription", p);
        return "prescription_create";
      }
      int patientId = rsPatient.getInt("id");

      /*
       * valid drug name
       */
      //TODO
      PreparedStatement psDrug = conn.prepareStatement(
          "SELECT drug_name FROM drug WHERE drug_name = ?");
      psDrug.setString(1, p.getDrugName());
      ResultSet rsDrug = psDrug.executeQuery();
      if (!rsDrug.next()) {
        model.addAttribute("message", "Drug not found. Check if"
            + " drug name is correct.");
        model.addAttribute("prescription", p);
        return "prescription_create";
      }
      String drugName = rsDrug.getString("drug_name");
      /*
       * insert prescription
       */
      //TODO
      PreparedStatement psPrescription = conn.prepareStatement(
          "INSERT INTO prescription (drug_name, quantity, patient_id, doctor_id, refill, date_prescribed) "
              + "VALUES (?, ?, ?, ?, ?, CURRENT_DATE)",
          PreparedStatement.RETURN_GENERATED_KEYS);
      psPrescription.setString(1, drugName);
      psPrescription.setInt(2, p.getQuantity());
      psPrescription.setInt(3, patientId);
      psPrescription.setInt(4, doctorId);
      psPrescription.setDate(5, java.sql.Date.valueOf(LocalDate.now()));
      psPrescription.setInt(5, p.getRefills());
      int rc = psPrescription.executeUpdate();


      ResultSet rsPrescription = psPrescription.getGeneratedKeys();
      if (rsPrescription.next()) {
        p.setRxid(rsPrescription.getInt(1));
      }

      if (rc == 1) {
        model.addAttribute("message", "Prescription created successfully.");
        model.addAttribute("prescription", p);
        return "prescription_show";
      } else {
        model.addAttribute("message", "Error creating prescription.");
        model.addAttribute("prescription", p);
        return "prescription_show";
      }


    } catch (
        SQLException e) {
      model.addAttribute("message", "SQL Error.: " + e.getMessage());
      return "prescription_create";
    }
  }
	
	private Connection getConnection() throws SQLException {
		Connection conn = jdbcTemplate.getDataSource().getConnection();
		return conn;
	}

}
