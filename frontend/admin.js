document.addEventListener("DOMContentLoaded", () => {
  const treeForm = document.getElementById("tree-form");
  const pendingTreesDiv = document.getElementById("pending-trees");

  // Fetch trees from backend and show them
  async function fetchTrees() {
    try {
      const response = await fetch("http://localhost:8080/api/trees/list");
      const trees = await response.json();

      pendingTreesDiv.innerHTML = "";

      if (trees.length === 0) {
        pendingTreesDiv.innerHTML = "<p>No trees submitted yet.</p>";
        return;
      }

      const list = document.createElement("ul");

      trees.forEach((tree) => {
        const item = document.createElement("li");
        item.textContent = `${tree.name} — (${tree.latitude}, ${tree.longitude})`;
        list.appendChild(item);
      });

      pendingTreesDiv.appendChild(list);
    } catch (error) {
      console.error("Error fetching trees:", error);
      pendingTreesDiv.innerHTML = "<p>Error loading trees.</p>";
    }
  }

  // Submit form to backend
  treeForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    const name = document.getElementById("treeName").value;
    const latitude = document.getElementById("latitude").value;
    const longitude = document.getElementById("longitude").value;
    const imageInput = document.getElementById("treeImage");

    if (!imageInput.files.length) {
      alert("Please select an image file.");
      return;
    }

    const formData = new FormData();
    formData.append("name", name);
    formData.append("latitude", latitude);
    formData.append("longitude", longitude);
    formData.append("image", imageInput.files[0]);

    try {
      const response = await fetch("http://localhost:8080/api/trees/add", {
        method: "POST",
        body: formData,
      });

      if (response.ok) {
        alert("Tree added successfully!");
        treeForm.reset();
        fetchTrees(); // refresh list
      } else {
        alert("Failed to add tree. Server error.");
      }
    } catch (error) {
      console.error("Error submitting tree:", error);
      alert("An error occurred while adding the tree.");
    }
  });

  fetchTrees();
});
