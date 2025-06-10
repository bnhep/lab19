package application;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import view.*;

/*
 * Controller class for patient interactions.
 *   register as a new patient.
 *   update patient profile.
 */
@Controller
public class ControllerPatientCreate {

  @Autowired
  private JdbcTemplate jdbcTemplate;


  /*
   * Request blank patient registration form.
   */
  @GetMapping("/patient/new")
  public String getNewPatientForm(Model model) {
    // return blank form for new patient registration
    model.addAttribute("patient", new PatientView());
    return "patient_register";
  }

  /*
   * Process data from the patient_register form
   */
  @PostMapping("/patient/new")
  public String createPatient(PatientView p, Model model) {

    /*
     * validate doctor last name and find the doctor id
     *
     */
    try (Connection con = getConnection()) {
      PreparedStatement psDoctor = con.prepareStatement(
          "SELECT id FROM doctor WHERE last_name = ?");
      psDoctor.setString(1, p.getPrimaryName());
      ResultSet rsDoctor = psDoctor.executeQuery();
      if (!rsDoctor.next()) {
        model.addAttribute("message", "Doctor not found.");
        model.addAttribute("patient", p);
        return "patient_register";
      }
      int doctorId = rsDoctor.getInt("id");
      /*
       * insert to patient table
       */
      PreparedStatement ps = con.prepareStatement(
          "INSERT INTO patient(first_name, last_name, birthdate, street, city, state, zipcode, doctor_id, ssn) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
          PreparedStatement.RETURN_GENERATED_KEYS
      );
      ps.setString(1, p.getFirst_name());
      ps.setString(2, p.getLast_name());
      ps.setDate(3, java.sql.Date.valueOf(p.getBirthdate()));
      ps.setString(4, p.getStreet());
      ps.setString(5, p.getCity());
      ps.setString(6, p.getState());
      ps.setString(7, p.getZipcode());
      ps.setInt(8, doctorId);
      ps.setString(9, p.getSsn());
      ps.executeUpdate();

      ResultSet rsPatient = ps.getGeneratedKeys();
      if (rsPatient.next()) {
        p.setId(rsPatient.getInt(1)); // adjust setter as needed
      }
      // display patient data and the generated patient ID,  and success message
      model.addAttribute("message", "Registration successful.");
      model.addAttribute("patient", p);
      return "patient_show";


      /*
       * on error
       * model.addAttribute("message", some error message);
       * model.addAttribute("patient", p);
       * return "patient_register";
       */
    } catch (SQLException e) {
      model.addAttribute("message", "SQL Error.: " + e.getMessage());
      model.addAttribute("patient", p);
      return "patient_register";
    }
  }

  /*
   * Request blank form to search for patient by id and name
   */
  @GetMapping("/patient/edit")
  public String getSearchForm(Model model) {
    model.addAttribute("patient", new PatientView());
    return "patient_get";
  }

  /*
   * Perform search for patient by patient id and name.
   */
  @PostMapping("/patient/show")
  public String showPatient(PatientView p, Model model) {

    // TODO   search for patient by id and name
    try (Connection con = getConnection();) {
      // if found, return "patient_show", else return error message and "patient_get"
      PreparedStatement psPatient = con.prepareStatement(
          "SELECT id, first_name, last_name, birthdate, street, city, state, zipcode, "
              + "doctor_id, ssn from patient where id = ? and last_name = ?");
      psPatient.setInt(1, p.getId());
      psPatient.setString(2, p.getLast_name());
      ResultSet rsPatient = psPatient.executeQuery();
      if (rsPatient.next()) {
        p.setId(rsPatient.getInt(1));
        p.setFirst_name(rsPatient.getString(2));
        p.setLast_name(rsPatient.getString(3));
        p.setBirthdate(rsPatient.getString(4).toString());
        p.setStreet(rsPatient.getString(5));
        p.setCity(rsPatient.getString(6));
        p.setState(rsPatient.getString(7));
        p.setZipcode(rsPatient.getString(8));
        PreparedStatement ps = con.prepareStatement(
            "SELECT d.last_name, d.id FROM patient p JOIN doctor d ON "
                + "p.doctor_id = d.id WHERE p.id = ?",
            PreparedStatement.RETURN_GENERATED_KEYS);
        ps.setInt(1, p.getId());
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
          p.setPrimaryName(rs.getString(1));
        } else {
          model.addAttribute("message", "Patient not found.");
          model.addAttribute("patient", p);
          return "patient_get";
        }
        model.addAttribute("patient", p);
        return "patient_show";

      } else {
        model.addAttribute("message", "Patient not found.");
        model.addAttribute("patient", p);
        return "patient_get";
      }

    } catch (SQLException e) {
      model.addAttribute("message", "SQL Error.: " + e.getMessage());
      model.addAttribute("patient", p);
      return "patient_get";
    }
  }

  /*
   * return JDBC Connection using jdbcTemplate in Spring Server
   */

  private Connection getConnection() throws SQLException {
    Connection conn = jdbcTemplate.getDataSource().getConnection();
    return conn;
  }
}
