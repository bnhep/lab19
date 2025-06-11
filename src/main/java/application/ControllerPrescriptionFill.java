package application;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import view.*;

@Controller
public class ControllerPrescriptionFill {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	//Patient requests form to fill prescription.
	@GetMapping("/prescription/fill")
	public String getfillForm(Model model) {
		model.addAttribute("prescription", new PrescriptionView());
		return "prescription_fill";
	}

	// process data from prescription_fill form
	@PostMapping("/prescription/fill")
	public String processFillForm(PrescriptionView p, Model model) {

		System.out.println("processFillForm " + p);
		try (Connection conn = getConnection();) {

			// valid pharmacy name and address, get pharmacy id and phone
			PreparedStatement psPharmacy = conn.prepareStatement(
					"SELECT id, phone_number FROM pharmacy WHERE name = ? AND address = ?");
			psPharmacy.setString(1, p.getPharmacyName());
			psPharmacy.setString(2, p.getPharmacyAddress());
			ResultSet rsPharmacy = psPharmacy.executeQuery();
			if (!rsPharmacy.next()) {
				model.addAttribute("message", "Pharmacy not found. Check if "
						+ "pharmacy name and address are correct.");
				model.addAttribute("prescription", p);
				return "prescription_fill";
			}
			int pharmacyId = rsPharmacy.getInt("id");
			String pharmacyPhone = rsPharmacy.getString("phone_number");
			p.setPharmacyID(pharmacyId);
			p.setPharmacyPhone(pharmacyPhone);

			// find the prescription and patient information
			PreparedStatement psPrescription = conn.prepareStatement(
					"SELECT p.RXID, p.quantity, p.refill, p.doctor_id, p.patient_id, p.drug_name, p.date_prescribed, " +
							"pt.first_name as patient_first_name, pt.last_name as patient_last_name " +
							"FROM prescription p " +
							"JOIN patient pt ON p.patient_id = pt.id " +
							"WHERE p.RXID = ?");
			psPrescription.setInt(1, p.getRxid());
			ResultSet rsPrescription = psPrescription.executeQuery();
			if (!rsPrescription.next()) {
				model.addAttribute("message", "Prescription not found. Check if "
						+ "prescription ID is correct.");
				model.addAttribute("prescription", p);
				return "prescription_fill";
			}

			int quantity = rsPrescription.getInt("quantity");
			int allowedRefills = rsPrescription.getInt("refill");
			int doctorId = rsPrescription.getInt("doctor_id");
			int patientId = rsPrescription.getInt("patient_id");
			String drugName = rsPrescription.getString("drug_name");
			java.sql.Date datePrescribed = rsPrescription.getDate("date_prescribed");
			String patientFirstName = rsPrescription.getString("patient_first_name");
			String patientLastName = rsPrescription.getString("patient_last_name");

			// Verify the patient last name matches what was entered on the form
			if (!patientLastName.equalsIgnoreCase(p.getPatientLastName())) {
				model.addAttribute("message", "Patient name does not match prescription. " +
						"Expected: " + patientLastName + ", but got: " + p.getPatientLastName());
				model.addAttribute("prescription", p);
				return "prescription_fill";
			}

			p.setQuantity(quantity);
			p.setRefills(allowedRefills);
			p.setDoctor_id(doctorId);
			p.setPatient_id(patientId);
			p.setDrugName(drugName);
			p.setDateCreated(datePrescribed.toString());
			p.setPatientFirstName(patientFirstName);
			p.setPatientLastName(patientLastName);

			PreparedStatement psFillCount = conn.prepareStatement(
					"SELECT COUNT(*) as fill_count FROM prescription_fill WHERE rxid = ?");
			psFillCount.setInt(1, p.getRxid());
			ResultSet rsFillCount = psFillCount.executeQuery();
			rsFillCount.next();
			int currentFills = rsFillCount.getInt("fill_count");

			if (currentFills > allowedRefills) {
				model.addAttribute("message", "Cannot fill prescription. Maximum number of refills ("
						+ allowedRefills + ") has been exceeded.");
				model.addAttribute("prescription", p);
				return "prescription_fill";
			}

			// get doctor information
			PreparedStatement psDoctor = conn.prepareStatement(
					"SELECT first_name, last_name, ssn, specialty, practice_since " +
							"FROM doctor WHERE id = ?");
			psDoctor.setInt(1, doctorId);
			ResultSet rsDoctor = psDoctor.executeQuery();
			if (!rsDoctor.next()) {
				model.addAttribute("message", "Doctor not found for this prescription.");
				model.addAttribute("prescription", p);
				return "prescription_fill";
			}
			p.setDoctorFirstName(rsDoctor.getString("first_name"));
			p.setDoctorLastName(rsDoctor.getString("last_name"));


			// calculate cost of prescription
			PreparedStatement psDrugCost = conn.prepareStatement(
					"SELECT unit_amount, price FROM drug_cost " +
							"WHERE drug_name = ? AND pharmacy_id = ? " +
							"ORDER BY CAST(SUBSTRING_INDEX(unit_amount, ' ', 1) AS UNSIGNED) DESC");
			psDrugCost.setString(1, drugName);
			psDrugCost.setInt(2, pharmacyId);
			ResultSet rsDrugCost = psDrugCost.executeQuery();

			double totalCost = 0.0;
			int remainingQuantity = quantity;
			boolean foundPricing = false;

			while (rsDrugCost.next() && remainingQuantity > 0) {
				foundPricing = true;
				String unitAmount = rsDrugCost.getString("unit_amount");
				double price = rsDrugCost.getDouble("price");

				int unitSize = Integer.parseInt(unitAmount.split(" ")[0]);

				if (remainingQuantity >= unitSize) {
					int unitsNeeded = remainingQuantity / unitSize;
					totalCost += unitsNeeded * price;
					remainingQuantity = remainingQuantity % unitSize;
				}
			}

			if (!foundPricing) {
				model.addAttribute("message", "Drug pricing not available at selected pharmacy.");
				model.addAttribute("prescription", p);
				return "prescription_fill";
			}

			if (remainingQuantity > 0) {
				psDrugCost = conn.prepareStatement(
						"SELECT unit_amount, price FROM drug_cost " +
								"WHERE drug_name = ? AND pharmacy_id = ? " +
								"ORDER BY CAST(SUBSTRING_INDEX(unit_amount, ' ', 1) AS UNSIGNED) ASC LIMIT 1");
				psDrugCost.setString(1, drugName);
				psDrugCost.setInt(2, pharmacyId);
				ResultSet rsSmallest = psDrugCost.executeQuery();
				if (rsSmallest.next()) {
					String unitAmount = rsSmallest.getString("unit_amount");
					double price = rsSmallest.getDouble("price");
					int unitSize = Integer.parseInt(unitAmount.split(" ")[0]);
					double pricePerUnit = price / unitSize;
					totalCost += remainingQuantity * pricePerUnit;
				}
			}

			// save updated prescription fill
			PreparedStatement psFill = conn.prepareStatement(
					"INSERT INTO prescription_fill (rxid, pharmacy_id, date_filled, fill_price) " +
							"VALUES (?, ?, CURRENT_DATE, ?)",
					PreparedStatement.RETURN_GENERATED_KEYS);
			psFill.setInt(1, p.getRxid());
			psFill.setInt(2, pharmacyId);
			psFill.setDouble(3, totalCost);

			int rc = psFill.executeUpdate();

			if (rc == 1) {
				ResultSet rsFillId = psFill.getGeneratedKeys();
				if (rsFillId.next()) {
				}
				p.setCost(String.format("%.2f", totalCost));
				p.setDateFilled(LocalDate.now().toString());
				p.setRefillsRemaining(allowedRefills - currentFills);

				// show the updated prescription with the most recent fill information
				model.addAttribute("message", "Prescription filled successfully. Cost: $" +
						String.format("%.2f", totalCost));
				model.addAttribute("prescription", p);
				return "prescription_show";
			} else {
				model.addAttribute("message", "Error filling prescription.");
				model.addAttribute("prescription", p);
				return "prescription_fill";
			}

		} catch (SQLException e) {
			model.addAttribute("message", "SQL Error: " + e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_fill";
		} catch (NumberFormatException e) {
			model.addAttribute("message", "Error parsing drug pricing information.");
			model.addAttribute("prescription", p);
			return "prescription_fill";
		}
	}

	private Connection getConnection() throws SQLException {
		Connection conn = jdbcTemplate.getDataSource().getConnection();
		return conn;
	}
}