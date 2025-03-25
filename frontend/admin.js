document.addEventListener("DOMContentLoaded", () => {
    const treeForm = document.getElementById("treeForm");
    const pendingTreesList = document.getElementById("pendingTreesList");
  
    // Fetch and display pending trees (you can update this if you add approval logic later)
    async function fetchPendingTrees() {
      try {
        const response = await fetch("http://localhost:8080/api/trees/list");
        const trees = await response.json();
  
        pendingTreesList.innerHTML = "";
  
        trees.forEach((tree) => {
          const li = document.createElement("li");
          li.textContent = `${tree.name} — (${tree.latitude}, ${tree.longitude})`;
          pendingTreesList.appendChild(li);
        });
      } catch (error) {
        console.error("Error fetching trees:", error);
      }
    }
  
    // Handle tree form submission (JSON-only for now)
    treeForm.addEventListener("submit", async (event) => {
      event.preventDefault();
  
      const treeName = document.getElementById("treeName").value;
      const latitude = parseFloat(document.getElementById("latitude").value);
      const longitude = parseFloat(document.getElementById("longitude").value);
  
      if (treeName && latitude && longitude) {
        const treeData = {
          name: treeName,
          latitude: latitude,
          longitude: longitude
        };
  
        try {
          const response = await fetch("http://localhost:8080/api/trees/add", {
            method: "POST",
            headers: {
              "Content-Type": "application/json"
            },
            body: JSON.stringify(treeData)
          });
  
          if (response.ok) {
            alert("Tree added successfully!");
            treeForm.reset();
            fetchPendingTrees(); // Refresh list
          } else {
            alert("Failed to add tree.");
          }
        } catch (error) {
          console.error("Error submitting tree:", error);
          alert("An error occurred while adding the tree.");
        }
      } else {
        alert("Please fill out all fields.");
      }
    });
  
    fetchPendingTrees(); // Initial load
  });
  