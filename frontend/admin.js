document.getElementById("tree-form").addEventListener("submit", async function (event) {
  event.preventDefault();

  const treeName = document.getElementById("treeName").value;
  const latitude = document.getElementById("latitude").value;
  const longitude = document.getElementById("longitude").value;
  const imageInput = document.getElementById("treeImage");
  const imageFile = imageInput.files[0];

  const formData = new FormData();
  formData.append("treeName", treeName);
  formData.append("latitude", latitude);
  formData.append("longitude", longitude);

  if (imageFile) {
    formData.append("treeImage", imageFile);
  }

  try {
    const response = await fetch("http://localhost:8080/api/trees/add", {
      method: "POST",
      body: formData,
    });

    if (response.ok) {
      alert("Tree submitted successfully!");
      document.getElementById("tree-form").reset();
    } else {
      alert("Failed to submit tree. Server responded with error.");
    }
  } catch (error) {
    console.error("Error submitting tree:", error);
    alert("An error occurred while submitting the tree.");
  }
});
