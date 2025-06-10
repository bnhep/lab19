package application;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import view.*;

/*
 * Controller class for patient interactions.
 *   register as a new patient.
 *   update patient profile.
 */
@Controller
public class ControllerPatientUpdate {

  @Autowired
  private JdbcTemplate jdbcTemplate;

  /*
   *  Display patient profile for patient id.
   */
  @GetMapping("/patient/edit/{id}")
  public String getUpdateForm(@PathVariable int id, Model model) {

    PatientView pv = new PatientView();
    // TODO search for patient by id
    pv.setId(id);
    try (Connection con = getConnection();) {
      PreparedStatement ps = con.prepareStatement(
          "SELECT pa.id, pa.first_name, pa.last_name, pa.birthdate, pa.street, " +
              "pa.city, pa.state, pa.zipcode, pa.ssn, d.last_name AS doctor_last_name " +
              "FROM patient pa JOIN doctor d ON pa.doctor_id = d.id " +
              "WHERE pa.id = ? ");
      ps.setInt(1, id);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        pv.setFirst_name(rs.getString(2));
        pv.setLast_name(rs.getString(3));
        pv.setBirthdate(rs.getString(4));
        pv.setStreet(rs.getString(5));
        pv.setCity(rs.getString(6));
        pv.setState(rs.getString(7));
        pv.setZipcode(rs.getString(8));
        pv.setPrimaryName(rs.getString(10));
        model.addAttribute("patient", pv);
        return "patient_edit";

      } else {
        // patient not found, return to home page
        model.addAttribute("message", "Patient not found.");
        model.addAttribute("patient", pv);
        return "index";
      }
      //  if not found, return to home page using return "index";
      //  else create PatientView and add to model.
      // model.addAttribute("message", some message);
      // model.addAttribute("patient", pv
      // return editable form with patient data
    } catch (SQLException e) {
      model.addAttribute("message", "SQL Error.:" + e.getMessage());
      model.addAttribute("patient", pv);
      return "patient_edit";
    }
  }


  /*
   * Process changes from patient_edit form
   *  Primary doctor, street, city, state, zip can be changed
   *  ssn, patient id, name, birthdate, ssn are read only in template.
   */
  @PostMapping("/patient/edit")
  public String updatePatient(PatientView p, Model model) {

    // validate doctor last name
    try (Connection con = getConnection();) {
      PreparedStatement psDoctor = con.prepareStatement(
          "SELECT id FROM doctor WHERE last_name = ?");
      psDoctor.setString(1, p.getPrimaryName());
      ResultSet rsDoctor = psDoctor.executeQuery();
      if (!rsDoctor.next()) {
        model.addAttribute("message", "Doctor not found.");
        model.addAttribute("patient", p);
        return "patient_edit";
      }
      int doctorId = rsDoctor.getInt("id");

      PreparedStatement ps = con.prepareStatement("UPDATE patient " +
          "SET doctor_id = ?, street = ?, city = ?, state = ?, zipcode = ? " +
          "WHERE id = ?");
      ps.setInt(1, doctorId);
      ps.setString(2, p.getStreet());
      ps.setString(3, p.getCity());
      ps.setString(4, p.getState());
      ps.setString(5, p.getZipcode());
      ps.setInt(6, p.getId());
      int rowsUpdated = ps.executeUpdate();
      if (rowsUpdated == 1) {
        // update successful, return to patient profile
        model.addAttribute("message", "Update successful.");
        model.addAttribute("patient", p);
        return "patient_show";
      } else {
        // update failed, return to edit form with error message
        model.addAttribute("message", "Error. Update was not successful.");
        model.addAttribute("patient", p);
        return "patient_edit";
      }
      // model.addAttribute("message", some message);
      // model.addAttribute("patient", p)

    } catch (SQLException e) {
      System.out.println("SQL error in updatePatient " + e.getMessage());
      model.addAttribute("message", "SQL Error.:" + e.getMessage());
      model.addAttribute("patient", p);
      return "patient_edit";
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
