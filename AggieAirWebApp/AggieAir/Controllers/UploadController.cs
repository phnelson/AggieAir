using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;

using AggieAir.Services;
using AggieAir.Models;
using Microsoft.EntityFrameworkCore.Metadata.Internal;
using System.IO;
using System.Text;

namespace AggieAir.Controllers
{
    public class UploadController : Controller
    {
        private readonly DataService dataService;

        public UploadController(DataService dataService)
        {
            this.dataService = dataService;
        }



        // GET: Upload
        public ActionResult Index()
        {
            System.Diagnostics.Debug.WriteLine("\n\nINDEX\n\n");
            return View();
        }













        public async Task<string> ReadTextAsync(string filePath)
        {
            using (FileStream sourceStream = new FileStream(filePath,
                FileMode.Open, FileAccess.Read, FileShare.Read,
                bufferSize: 4096, useAsync: true))
            {
                StringBuilder sb = new StringBuilder();

                byte[] buffer = new byte[0x1000];
                int numRead;
                while ((numRead = await sourceStream.ReadAsync(buffer, 0, buffer.Length)) != 0)
                {
                    string text = Encoding.Unicode.GetString(buffer, 0, numRead);
                    sb.Append(text);
                }

                string temp = sb.ToString();

                string[] PM_line = temp.Split(new[] { ' ' }, StringSplitOptions.RemoveEmptyEntries);

                char[] trimChars = { ' ', 'P', 'M' };
                string PM25 = PM_line[4].Trim(trimChars);
                PM25 = PM25.Trim();

                //var extractedPM = parseInt(PM_line[5]); // extracts PM
                var extractedPM = Int32.Parse(PM25);

                var model = new SensorData();
                model.PM10 = 0;
                model.PM25 = Convert.ToDecimal(extractedPM);
                model.PMTen = 0;
                model.GPGLL = "abc";



                dataService.Create(model);


                return sb.ToString();
            }
        }













        [HttpPost("Upload")]
        public async Task<IActionResult> Index(List<IFormFile> files)
        {
            long size = files.Sum(f => f.Length);
            System.Diagnostics.Debug.WriteLine("\n\n UPLOAD \n\n");
            var filePaths = new List<string>();

            // Will always be a single file!
            foreach (var formFile in files)
            {
                if (formFile.Length > 0)
                {
                    // full path to file in temp location
                    //var filePath = System.IO.Path.GetTempFileName(); //we are using Temp file name just for the example. Add your own file path.
                    //var filePath = "../Upload_files/";
                    var filePath = Path.Combine(Directory.GetCurrentDirectory(),
                     "Upload_files", formFile.FileName);
                    //System.Diagnostics.Debug.WriteLine(filePath);
                    filePaths.Add(filePath);

                    using (var stream = new System.IO.FileStream(filePath, System.IO.FileMode.Create))
                    {
                        await formFile.CopyToAsync(stream);
                    }

                    //if (System.IO.File.Exists(filePaths[0]) == false)
                    //{
                    //    System.Diagnostics.Debug.WriteLine("file not found: " + filePaths[0]);
                    //}
                    //else
                    //{
                    //    try
                    //    {
                    //        string text = await ReadTextAsync(filePaths[0]);
                    //        System.Diagnostics.Debug.WriteLine(text);
                    //    }
                    //    catch (Exception ex)
                    //    {
                    //        System.Diagnostics.Debug.WriteLine(ex.Message);
                    //    }
                    //}
                }
            }

            //string fpath = System.IO.File.ReadAllText(filePaths[0]);









            
            string textFile = System.IO.File.ReadAllText(filePaths[0]);
            //System.Diagnostics.Debug.WriteLine(textFile);


            //string[] lines = textFile.Split("/[\r\n] +/ g"); // Windows and Unix linebreak support
            string[] lines = textFile.Split(new[] { Environment.NewLine },StringSplitOptions.None);

            int counter = 0; // Total documents created

            //System.Diagnostics.Debug.WriteLine(lines[0]);



            
            int i = -1;

            System.Diagnostics.Debug.WriteLine(lines.Length);

            while (i < lines.Length - 1)
            {

                i++;


                //System.Diagnostics.Debug.WriteLine("Parsing: " + lines[i]);



                if (lines[i][0] == 'P' && (lines[i + 1][0] == '"' || lines[i + 1][0] == '$'))
                { // Valid raw data pair


                    //var PM_line = lines[i].match(/\S +/ g);
                    //string[] PM_line = lines[i].Split(new[] { ' ' }, StringSplitOptions.RemoveEmptyEntries);
                    string[] PM_line = lines[i].Split(new[] { ' ' }, StringSplitOptions.RemoveEmptyEntries);

                    //foreach (var item in PM_line)
                    //{
                    //    Console.WriteLine(item);
                    //}

                    char[] trimChars = { ' ', 'P', 'M' };
                    string PM25 = PM_line[4].Trim(trimChars);
                    PM25 = PM25.Trim();

                    //var extractedPM = parseInt(PM_line[5]); // extracts PM
                    var extractedPM = Int32.Parse(PM25);

                    //System.Diagnostics.Debug.WriteLine(extractedPM);

                    //var GPGLL_line = lines[i + 1];
                    string GPGLL_line = lines[i + 1];

                    var model = new SensorData();
                    model.PM10 = 0;
                    model.PM25 = Convert.ToDecimal(extractedPM);
                    model.PMTen = 0;

                    //"$GPGLL,3832.5980,N,12145.0612,W,234545.000,A,A*42"

                    string[] GPGLL_split = GPGLL_line.Split(new Char[] { ',' }, StringSplitOptions.RemoveEmptyEntries);
                    //Console.WriteLine(GPGLL_split[1]);

                    string lat_rough = GPGLL_split[1];
                    string lng_rough = GPGLL_split[3];

                    lat_rough = (decimal.Parse(lat_rough) / 100m).ToString();
                    lng_rough = (decimal.Parse(lng_rough) / 100m).ToString();

                    string[] lat_split = lat_rough.Split('.');
                    string DD1 = lat_split[0];
                    string MMMM1 = lat_split[1];
                    MMMM1 = ((decimal.Parse(MMMM1)) / 60).ToString();

                    string[] lng_split = lng_rough.Split('.');
                    string DD2 = lng_split[0];
                    string MMMM2 = lng_split[1];
                    MMMM2 = ((decimal.Parse(MMMM2)) / 60).ToString();

                    // lat is positive
                    // lng is to be inverted
                    string lat = DD1 + "." + ((int)(decimal.Parse(MMMM1) * 1000)).ToString();
                    string lng = "-" + DD2 + "." + ((int)(decimal.Parse(MMMM2) * 1000)).ToString();

                    //Console.WriteLine(lat);
                    //Console.WriteLine(lng);

                    string GPGLL = lat + "," + lng;
                    model.GPGLL = GPGLL;





                    dataService.Create(model);
                    counter++;
                }
            }

        


            if (textFile.Length > 0)
            {
                System.Diagnostics.Debug.WriteLine("Found file at [0]! Deleting it.");
                System.Diagnostics.Debug.WriteLine(filePaths[0]);
                System.IO.File.Delete(filePaths[0]);
            }


            // process uploaded files
            // Don't rely on or trust the FileName property without validation.
            System.Diagnostics.Debug.WriteLine("Documents created:");
            System.Diagnostics.Debug.WriteLine(counter);

            

            //return Ok(new { count = files.Count, size, filePaths });
            return View();
        }


        //// GET: Upload/Details/5
        //public ActionResult Details(int id)
        //{
        //    return View();
        //}

        //// GET: Upload/Create
        //public ActionResult Create()
        //{
        //    return View();
        //}

        //// POST: Upload/Create
        //[HttpPost]
        //[ValidateAntiForgeryToken]
        //public ActionResult Create(IFormCollection collection)
        //{
        //    try
        //    {
        //        // TODO: Add insert logic here

        //        return RedirectToAction(nameof(Index));
        //    }
        //    catch
        //    {
        //        return View();
        //    }
        //}

        //// GET: Upload/Edit/5
        //public ActionResult Edit(int id)
        //{
        //    return View();
        //}

        //// POST: Upload/Edit/5
        //[HttpPost]
        //[ValidateAntiForgeryToken]
        //public ActionResult Edit(int id, IFormCollection collection)
        //{
        //    try
        //    {
        //        // TODO: Add update logic here

        //        return RedirectToAction(nameof(Index));
        //    }
        //    catch
        //    {
        //        return View();
        //    }
        //}

        //// GET: Upload/Delete/5
        //public ActionResult Delete(int id)
        //{
        //    return View();
        //}

        //// POST: Upload/Delete/5
        //[HttpPost]
        //[ValidateAntiForgeryToken]
        //public ActionResult Delete(int id, IFormCollection collection)
        //{
        //    try
        //    {
        //        // TODO: Add delete logic here

        //        return RedirectToAction(nameof(Index));
        //    }
        //    catch
        //    {
        //        return View();
        //    }
        //}
    }
}